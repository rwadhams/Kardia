package com.wadhams.kardia.dto

import java.time.LocalDate

import groovy.transform.ToString

@ToString
class KardiaEvent {
	Event event
	LocalDate startDate
	LocalDate endDate	//range
	long days			//calculated based on date range
	
	List<String> descList
}

enum Event {
	Single,
	Range
}
