package com.ds.app.exception;

public class DailyHoursLimitExceededException extends RuntimeException{
	public DailyHoursLimitExceededException(String msg) {
		super(msg);
	}
}
