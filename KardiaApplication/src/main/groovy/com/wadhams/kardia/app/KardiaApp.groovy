package com.wadhams.kardia.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import com.wadhams.kardia.dto.Event
import com.wadhams.kardia.dto.KardiaEvent
import com.wadhams.kardia.dto.KardiaMedication
import com.wadhams.kardia.dto.KardiaReading
import com.wadhams.kardia.dto.KardiaSymptom
import com.wadhams.kardia.dto.ListRange

import groovy.util.logging.Log4j2

@Log4j2 (value = 'logger')
class KardiaApp {
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm')
	DateTimeFormatter df = DateTimeFormatter.ofPattern('dd/MM/yyyy')
	DateTimeFormatter dfOut = DateTimeFormatter.ofPattern('dd/LLL/yyyy')
	
	List<KardiaReading> krList
	List<ListRange> listRangeList
	List<KardiaMedication> kmList
	List<KardiaSymptom> ksList
	List<KardiaEvent> keList
	List<String> backgroundList
	
	long totalReportingPeriod	//days
	
	//blood pressure values
	int numberOfBloodPressureReadings
	KardiaReading maxBloodPressure
	KardiaReading minBloodPressure

	static main(args) {
		logger.info 'KardiaApp started...'

		KardiaApp app = new KardiaApp()
		app.execute()
		
		logger.info 'KardiaApp ended.'
	}
	
	def execute() {
		String kFilename = 'Kardia.xml'
		logger.info "Processing: $kFilename"
		
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
		logger.debug krList
		
		listRangeList = buildListRange()
		logger.debug listRangeList
		
		kmList = buildKardiaMedicationList(k.medication)
		logger.debug kmList
		
		ksList = buildKardiaSymptomList(k.symptom)
		logger.debug ksList
		
		backgroundList = buildBackgroundList(k.timeline.background)
		logger.debug backgroundList
		
		keList = buildKardiaEventList(k.timeline.event)
		logger.debug keList
		
		totalReportingPeriod = ChronoUnit.DAYS.between(krList[0].dateTime.toLocalDate(), krList[-1].dateTime.toLocalDate()) + 1
		logger.debug "totalReportingPeriod: $totalReportingPeriod"

		determineBloodPressureValues()	//size, max and min
				
		File f1 = new File("out/kardia-report.txt")
		f1.withPrintWriter {pw ->
			reportTotalReportingPeriod(pw)
			reportReadings(pw)
			reportBloodPressure(pw)
			reportMedications(pw)
			reportSymptoms(pw)
			reportBackground(pw)
			reportTimeline(pw)
		}

		File f2 = new File("out/kardia-report.html")
		f2.withPrintWriter {pw ->
			pw.println '<html>'
			reportTotalReportingPeriodHTML(pw)
			reportReadingsHTML(pw)
			reportBloodPressureHTML(pw)
			reportMedicationsHTML(pw)
			reportSymptomsHTML(pw)
			reportBackgroundHTML(pw)
			reportTimelineHTML(pw)
			pw.println '</html>'
		}

	}
	
	def reportTotalReportingPeriod(PrintWriter pw) {
		pw.println "Total reporting period: $totalReportingPeriod days."
		pw.println ''
	}
	
	def reportTotalReportingPeriodHTML(PrintWriter pw) {
		pw.println "<p>Total reporting period: $totalReportingPeriod days.</p>"
	}
	
	def reportReadings(PrintWriter pw) {
		listRangeList.each {rl ->
			List<KardiaReading> subList = krList.subList(rl.startIndex, rl.endIndex+1)
			logger.debug "reportReadings: $subList"
			def kMin = subList.min {it.rate}
			def avg = subList.average {it.rate}
			def kMax = subList.max {it.rate}
			String resultName = (subList[0].result == 'NSR') ? 'Normal Sinus Rhythm............: ' : 'Possible Atrial Fibrillation...: '
			String startDate = subList[0].dateTime.format(dtf)
			String endDate = subList[-1].dateTime.format(dtf)
			long days = ChronoUnit.DAYS.between(subList[0].dateTime.toLocalDate(), subList[-1].dateTime.toLocalDate()) + 1
			String average = (avg as int).toString().padRight(3, ' ')
			
			pw.println "$resultName${startDate} - ${endDate} (${days} days)\tHeartRate\tAverage: ${average}\tMin/Max: ${kMin.rate}/${kMax.rate}"
		}
		pw.println ''
	}
	
	def reportReadingsHTML(PrintWriter pw) {
		pw.println '<b>Readings:</b>'
		pw.println '<table border="1">'
		listRangeList.each {rl ->
			List<KardiaReading> subList = krList.subList(rl.startIndex, rl.endIndex+1)
			logger.debug "reportReadingsHTML: $subList"
			def kMin = subList.min {it.rate}
			def avg = subList.average {it.rate}
			def kMax = subList.max {it.rate}
			String resultName = (subList[0].result == 'NSR') ? 'Normal Sinus Rhythm' : 'Possible Atrial Fibrillation'
			String startDate = subList[0].dateTime.format(dtf)
			String endDate = subList[-1].dateTime.format(dtf)
			long days = ChronoUnit.DAYS.between(subList[0].dateTime.toLocalDate(), subList[-1].dateTime.toLocalDate()) + 1
			String average = (avg as int).toString().padRight(3, ' ')
			
			pw.println '<tr>'
			pw.println "<td>$resultName</td>"
			pw.println "<td>${startDate} - ${endDate}</td>"
			pw.println "<td>$days days</td>"
			pw.println "<td>HeartRate</td>"
			pw.println "<td>Average: ${average}</td>"
			pw.println "<td>Min/Max: ${kMin.rate}/${kMax.rate}</td>"
			pw.println '</tr>'
		}
		pw.println '</table>'
	}
	
	def reportBloodPressure(PrintWriter pw) {
		pw.println "Number of Blood Pressure readings: $numberOfBloodPressureReadings"
		pw.println "\tMin: ${minBloodPressure.dateTime.format(df)} (${minBloodPressure.systolic}/${minBloodPressure.diastolic})"
		pw.println "\tMax: ${maxBloodPressure.dateTime.format(df)} (${maxBloodPressure.systolic}/${maxBloodPressure.diastolic})"
		pw.println ''
	}
	
	def reportBloodPressureHTML(PrintWriter pw) {
		pw.println '<b>Blood Pressure:</b>'
		pw.println '<table border="1">'
		pw.println "<tr><td>Number of Blood Pressure readings:</td><td>$numberOfBloodPressureReadings</td></tr>"
		pw.println "<tr><td>Min read on ${minBloodPressure.dateTime.format(df)}</td><td>${minBloodPressure.systolic}/${minBloodPressure.diastolic}</td></tr>"
		pw.println "<tr><td>Max read on ${maxBloodPressure.dateTime.format(df)}</td><td>${maxBloodPressure.systolic}/${maxBloodPressure.diastolic}</td></tr>"
		pw.println '</table>'
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
			pw.println "Medication: ${m.name.padRight(maxMedicationLength, ' ')} Start: ${m.start.format(df)} End: ${m.end.format(df)} ($totalDays days)"
		}
		pw.println ''
	}
	
	def reportMedicationsHTML(PrintWriter pw) {
		pw.println '<b>Medication:</b>'
		pw.println '<table border="1">'
		kmList.each {m ->
			pw.println '<tr>'
			long totalDays = ChronoUnit.DAYS.between(m.start, m.end) + 1
			pw.println "<td>${m.name}</td>"
			pw.println "<td>Start: ${m.start.format(df)} End: ${m.end.format(df)} ($totalDays days)</td>"
			pw.println '</tr>'
		}
		pw.println '</table>'
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
			pw.println "Symptom: ${s.name.padRight(maxSymptomLength, ' ')} Result: ${s.result} Start: ${s.start.format(df)} End: ${s.end.format(df)} ($totalDays days)"
		}
		pw.println ''
	}
	
	def reportSymptomsHTML(PrintWriter pw) {
		pw.println '<b>Symptoms:</b>'
		pw.println '<table border="1">'
		ksList.each {s ->
			pw.println '<tr>'
			long totalDays = ChronoUnit.DAYS.between(s.start, s.end) + 1
			pw.println "<td>${s.name}</td>"
			pw.println "<td>Result: ${s.result} Start: ${s.start.format(df)} End: ${s.end.format(df)} ($totalDays days)</td>"
			pw.println '</tr>'
		}
		pw.println '</table>'
	}
	
	def reportBackground(PrintWriter pw) {
		pw.println 'Background:'
		backgroundList.each {bg ->
			pw.println "\t$bg"
		}
		pw.println ''
	}
	
	def reportBackgroundHTML(PrintWriter pw) {
		pw.println '<b>Background:</b>'
		pw.println '<table border="1">'
		backgroundList.each {bg ->
			pw.println "<tr><td>$bg</td></tr>"
		}
		pw.println '</table>'
	}
	
	def reportTimeline(PrintWriter pw) {
		pw.println 'Timeline:'
		keList.each {e ->
			if (e.event == Event.Single) {
				pw.println "Date: ${e.startDate.format(dfOut)}"
			}
			else {	//implies a range
				pw.println "Start: ${e.startDate.format(dfOut)}\tEnd: ${e.endDate.format(dfOut)}\t (${e.days} days.)"
			}
			e.descList.each {d ->
				pw.println "\t$d"
			}
			pw.println ''
		}
	}
	
	def reportTimelineHTML(PrintWriter pw) {
		pw.println '<b>Timeline:</b>'
		pw.println '<table border="1">'
		keList.each {e ->
			pw.println '<tr>'
			pw.println '<td>'
			if (e.event == Event.Single) {
				pw.println "${e.startDate.format(dfOut)}"
			}
			else {	//implies a range
				pw.println "${e.startDate.format(dfOut)}<br/ >${e.endDate.format(dfOut)}<br/ >(${e.days} days.)"
			}
			pw.println '</td>'
			pw.println '<td>'
			e.descList.each {d ->
				pw.println d
				pw.println '<br/ >'
			}
			pw.println '</td>'
			pw.println '</tr>'
		}
		pw.println '</table>'
	}
	
	def buildListRange() {
		List<ListRange> listRangeList = []
		
		String result = krList[0].result
		int startIndex = 0
		krList.eachWithIndex {k, i ->
			logger.debug "index=$i, value=$k"
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
			KardiaReading kr = new KardiaReading(dateTime : dateTime, rate : Integer.parseInt(rate), result : result)

			//Blood pressure is optional			
			String systolic = r.@systolic
			String diastolic = r.@diastolic
			if (systolic && diastolic) {
				kr.systolic = Integer.parseInt(systolic)
				kr.diastolic = Integer.parseInt(diastolic)
			}
			
			krList << kr
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
	
	List<KardiaEvent> buildKardiaEventList(events) {
		List<KardiaEvent> keList = []
		
		events.each {e ->
			KardiaEvent ke = new KardiaEvent()
			String eventDt = e.@eventDt.text()
			if (eventDt) {
				ke.event = Event.Single
				ke.startDate = LocalDate.parse(eventDt, df)
				ke.descList = []
				e.desc.each {d ->
					ke.descList << d
				} 				
			}
			else {	//implies a range
				ke.event = Event.Range
				ke.startDate = LocalDate.parse(e.@startDt.text(), df)
				String end = e.@endDt.text()
				if (end) {
					ke.endDate = LocalDate.parse(end, df)
				}
				else {
					ke.endDate = LocalDate.now()
				}
				ke.days = ChronoUnit.DAYS.between(ke.startDate, ke.endDate) + 1
				ke.descList = []
				e.desc.each {d ->
					ke.descList << d
				} 				
			}

			keList << ke
		}
		
		return keList
	}
	
	List<String> buildBackgroundList(background) {
		List<String> bgList = []
		
		background.desc.each {d ->
			bgList << d
		}
		
		return bgList
	}
	
	def determineBloodPressureValues() {
		List<KardiaReading> bpList = krList.findAll {kr ->
			kr.systolic > 0 && kr.diastolic > 0
		}
		logger.debug "bpList: $bpList"
		numberOfBloodPressureReadings = bpList.size()
		
		maxBloodPressure = bpList[0]
		minBloodPressure = bpList[0]
		bpList[1..-1].each {kr ->
			if (kr.systolic > maxBloodPressure.systolic) {
				maxBloodPressure = kr
			}
			if (kr.systolic < minBloodPressure.systolic) {
				minBloodPressure = kr
			}
		}
		logger.debug "max bp: $maxBloodPressure"
		logger.debug "min bp: $minBloodPressure"
	}
	
}
