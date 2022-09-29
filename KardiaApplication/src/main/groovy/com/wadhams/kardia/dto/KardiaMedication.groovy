package com.wadhams.kardia.dto

import java.time.LocalDate

import groovy.transform.ToString

@ToString
class KardiaMedication {
	String name
	LocalDate start
	LocalDate end
}
