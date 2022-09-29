package com.wadhams.kardia.dto

import java.time.LocalDate

import groovy.transform.ToString

@ToString
class KardiaSymptom {
	String name
	String result
	LocalDate start
	LocalDate end
}
