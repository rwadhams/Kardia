package com.wadhams.kardia.dto

import java.time.LocalDate
import java.time.LocalDateTime

import groovy.transform.ToString

@ToString
class ReportingValues {
	LocalDate reportingPeriodStartDate
	long totalReportingPeriod	//days
	
	//blood pressure values
	int numberOfBloodPressureReadings
	KardiaBloodPressure maxBloodPressure
	KardiaBloodPressure minBloodPressure
}
