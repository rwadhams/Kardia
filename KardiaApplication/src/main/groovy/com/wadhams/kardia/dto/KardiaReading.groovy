package com.wadhams.kardia.dto

import java.time.LocalDateTime

import groovy.transform.ToString

@ToString
class KardiaReading {
	LocalDateTime dateTime
	int rate
	String result
	
	//Blood pressure
	int systolic
	int diastolic
}
