package com.wadhams.kardia.app

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.wadhams.kardia.dto.KardiaMedication
import com.wadhams.kardia.dto.KardiaReading
import com.wadhams.kardia.dto.KardiaSymptom
import com.wadhams.kardia.dto.ListRange
import groovy.transform.ToString

class KardiaApp {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm')
	DateTimeFormatter df = DateTimeFormatter.ofPattern('dd/MM/yyyy')
	
	List<KardiaReading> krList
	List<ListRange> listRangeList
	List<KardiaMedication> kmList
	List<KardiaSymptom> ksList

	static main(args) {
		println 'KardiaApp started...'
		println ''

		KardiaApp app = new KardiaApp()
		app.execute()
		
		println ''
		println 'KardiaApp ended.'
	}
	
	def execute() {
		String kFilename = 'Kardia.xml'
		println "Processing: $kFilename"
		println ''
		
		File kFile
		URL resource = getClass().getClassLoader().getResource(kFilename)
		if (resource == null) {
			throw new IllegalArgumentException("File not found!")
		} 
		else {
			kFile = new File(resource.toURI())
		}
		
		def k = new XmlSlurper().parse(kFile)
		
		krList = buildKardiaReadingList(k.reading)
//		println krList
//		println ''
		
		listRangeList = buildListRange()
//		println listRangeList
//		println ''
		
		kmList = buildKardiaMedicationList(k.medication)
//		println kmList
//		println ''
		
		ksList = buildKardiaSymptomList(k.symptom)
//		println ksList
//		println ''
		
		File f = new File("out/kardia-report.txt")
		f.withPrintWriter {pw ->
			reportReadings(pw)
			reportTotals(pw)
			reportMedications(pw)
			reportSymptoms(pw)
		}

	}
	
	def reportReadings(PrintWriter pw) {
		DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern('dd/MM/yyyy')
		
		listRangeList.each {rl ->
			List<KardiaReading> subList = krList.subList(rl.startIndex, rl.endIndex+1)
			//println subList
			def kMin = subList.min {it.rate}
			def avg = subList.average {it.rate}
			def kMax = subList.max {it.rate}
			String resultName = (subList[0].result == 'NSR') ? 'Normal Sinus Rhythm............: ' : 'Possible Atrial Fibrillation...: '
			String startDate = subList[0].dateTime.format(dtf1)
			String endDate = subList[-1].dateTime.format(dtf1)
			long days = ChronoUnit.DAYS.between(subList[0].dateTime.toLocalDate(), subList[-1].dateTime.toLocalDate()) + 1
			String average = (avg as int).toString().padRight(3, ' ')
			
			pw.println "$resultName${startDate} - ${endDate} (${days} days)\tAverage: ${average}\tMin/Max: ${kMin.rate}/${kMax.rate}"
		}
		pw.println ''
	}
	
	def reportTotals(PrintWriter pw) {
		long totalDays = ChronoUnit.DAYS.between(krList[0].dateTime.toLocalDate(), krList[-1].dateTime.toLocalDate()) + 1
		
		long pafDays = 0
		listRangeList.each {rl ->
			List<KardiaReading> subList = krList.subList(rl.startIndex, rl.endIndex+1)
			if (subList[0].result == 'PAF') {
				pafDays += ChronoUnit.DAYS.between(subList[0].dateTime.toLocalDate(), subList[-1].dateTime.toLocalDate()) + 1
			}
		}
		pw.println "Total reporting period: $totalDays days. Possible Atrial Fibrillation: $pafDays days."
		pw.println ''
	}
	
	def reportMedications(PrintWriter pw) {
		int maxMedicationLength = 0
		kmList.each {m ->
			if (m.name.size() > maxMedicationLength) {
				maxMedicationLength = m.name.size()
			}
		}
		
		kmList.each {m ->
			long totalDays = ChronoUnit.DAYS.between(m.start, m.end) + 1
			pw.println "Medication: ${m.name.padRight(maxMedicationLength, ' ')} Start: ${m.start} End: ${m.end} ($totalDays days)"
		}
		pw.println ''
	}
	
	def reportSymptoms(PrintWriter pw) {
		int maxSymptomLength = 0
		ksList.each {s ->
			if (s.name.size() > maxSymptomLength) {
				maxSymptomLength = s.name.size()
			}
		}
		
		ksList.each {s ->
			long totalDays = ChronoUnit.DAYS.between(s.start, s.end) + 1
			pw.println "Symptom: ${s.name.padRight(maxSymptomLength, ' ')} Result: ${s.result} Start: ${s.start} End: ${s.end} ($totalDays days)"
		}
		pw.println ''
	}
	
	def buildListRange() {
		List<ListRange> listRangeList = []
		
		String result = krList[0].result
		int startIndex = 0
		krList.eachWithIndex {k, i ->
//			println k
//			println i
			if (result != k.result) {
				listRangeList << new ListRange(startIndex : startIndex, endIndex : i-1)
				startIndex = i
				result = k.result
			}
		}
		listRangeList << new ListRange(startIndex : startIndex, endIndex : krList.size()-1)
		
		return listRangeList
	}
	
	List<KardiaReading> buildKardiaReadingList(readings) {
		List<KardiaReading> krList = []
		
		readings.each {r ->
			LocalDateTime dateTime = LocalDateTime.parse(r.@datetime.text(), dtf)
			String rate = r.@rate
			String result = r.@result
			krList << new KardiaReading(dateTime : dateTime, rate : Integer.parseInt(rate), result : result)
		}
		
		return krList
	}
	
	List<KardiaMedication> buildKardiaMedicationList(medications) {
		List<KardiaMedication> kmList = []
		
		medications.each {m ->
			String name = m.@name
			LocalDate start = LocalDate.parse(m.@start.text(), df)
			LocalDate end = LocalDate.now()
			String endDate = m.@end.text()
			if (endDate) {
				end = LocalDate.parse(endDate, df)
			}
			
			kmList << new KardiaMedication(name : name, start : start, end : end)
		}
		
		return kmList
	}
	
	List<KardiaSymptom> buildKardiaSymptomList(symptoms) {
		List<KardiaSymptom> ksList = []
		
		symptoms.each {s ->
			String name = s.@name
			String result = s.@result
			LocalDate start = LocalDate.parse(s.@start.text(), df)
			LocalDate end = LocalDate.now()
			String endDate = s.@end.text()
			if (endDate) {
				end = LocalDate.parse(endDate, df)
			}
			
			ksList << new KardiaSymptom(name : name, result : result, start : start, end : end)
		}
		
		return ksList
	}
	
}
