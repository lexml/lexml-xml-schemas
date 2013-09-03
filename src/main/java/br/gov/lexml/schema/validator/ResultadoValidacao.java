package br.gov.lexml.schema.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class ResultadoValidacao implements ErrorHandler {
	public final List<SAXParseException> erros = new ArrayList<SAXParseException>();
	public final List<SAXParseException> errosFatais= new ArrayList<SAXParseException>();
	public final List<SAXParseException> avisos= new ArrayList<SAXParseException>();
	@Override
	public void warning(SAXParseException exception) throws SAXException {
		avisos.add(exception);
	}
	@Override
	public void error(SAXParseException exception) throws SAXException {
		erros.add(exception);
	}
	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		errosFatais.add(exception);
	}		
	private static <T> void toStringList(StringBuilder sb, List<T> l) {		
		Iterator<T> i = l.iterator();
		if(i.hasNext()) {
			sb.append(i.next());
		}
		while(i.hasNext()) {
			sb.append(", ");
			sb.append(i.next());
		}		
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();				
		sb.append("ResultadoValidacao@" + Long.toHexString(super.hashCode()) + "(");
		sb.append("erros = ");
		toStringList(sb,erros);
		sb.append("; errosFatais = ");
		toStringList(sb,errosFatais);
		sb.append("; avisos = ");
		toStringList(sb,avisos);
		sb.append(")");
		return sb.toString();
	}
}
