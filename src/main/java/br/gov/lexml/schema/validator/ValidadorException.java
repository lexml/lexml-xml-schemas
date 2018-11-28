package br.gov.lexml.schema.validator;

public class ValidadorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	ValidadorException(String message) {
		super(message);

	}

	ValidadorException(String message, Throwable cause) {
		super(message, cause);
	}

}
