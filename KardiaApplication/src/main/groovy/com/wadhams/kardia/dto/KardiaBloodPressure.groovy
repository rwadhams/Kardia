package com.wadhams.kardia.dto

import java.time.LocalDateTime

import groovy.transform.ToString

@ToString
class KardiaBloodPressure {
	LocalDateTime dateTime
	int systolic
	int diastolic
}
