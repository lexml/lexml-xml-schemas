package br.gov.lexml.schema.validator.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import br.gov.lexml.schema.validator.ResultadoValidacao;
import br.gov.lexml.schema.validator.TipoSchema;
import br.gov.lexml.schema.validator.Validador;
import br.gov.lexml.schema.validator.ValidadorException;

public class ValidadorTest {
	private static final Logger logger = LoggerFactory.getLogger(ValidadorTest.class);
	
	
	private static final class Expectativa {
		public final TipoSchema tipo;		
		public final boolean devemHaverErrosFatais;
		public final boolean devemHaverErros;
		public final boolean devemHaverAvisos;
		public Expectativa(TipoSchema tipo, boolean devemHaverErrosFatais,
				boolean devemHaverErros, boolean devemHaverAvisos) {
			super();
			this.tipo = tipo;
			this.devemHaverErrosFatais = devemHaverErrosFatais;
			this.devemHaverErros = devemHaverErros;
			this.devemHaverAvisos = devemHaverAvisos;
		}
		@SuppressWarnings("unused")
		public Expectativa(TipoSchema tipo) {
			this(tipo,false,false,false);
		}
		public String toString() {
			return "Expectativa(" + tipo + "," + devemHaverErrosFatais + "," +
				devemHaverErros + "," + devemHaverAvisos + ")";
		}
	}
	
	private static class Padrao {
		public final Pattern pattern;		
		public final Expectativa expectativa;
		public Padrao(Pattern pattern, Expectativa expectativa) {		
			this.pattern = pattern;
			this.expectativa = expectativa;
		}
		public String toString() {
			return "Padrao(" + pattern.pattern() + "," + expectativa + ")";
		}
	}
	
	private static class Exemplo {
		public final Expectativa expectativa;
		public final String path;
		public Exemplo(Expectativa expectativa, String path) {		
			this.expectativa = expectativa;
			this.path = path;
		}		
		public String toString() {
			return "Exemplo(" + expectativa + "," + path + ")";
		}
	}
			
	@Test
	public void testGetInstance() throws ValidadorException {
		logger.info("testGetInstance");
		assertNotNull("Validador.getInstance não deve retornar nulo",Validador.getInstance()); 
	}
	
	private static final String testPadroesFileName = "test-padroes.txt";
	private static final String listaExemplosFileName = "lista-exemplos.txt";
	
	public List<Padrao> obtemPadroes() throws IOException {
		InputStream is = ValidadorTest.class.getClassLoader().getResourceAsStream(testPadroesFileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		List<Padrao> padroes = new LinkedList<Padrao>();		
		while(true) {
			String line = br.readLine();
			if(line == null) break;
			if(line.startsWith("#")) continue;
			String[] comps = line.split(",");
			if(comps.length < 5) {
				logger.warn(testPadroesFileName + ": linha com menos de 5 componentes: " + line);
				continue;
			} else if (comps.length > 5) {
				logger.warn(testPadroesFileName + ": linha com mais de 5 componentes: " + line);				
			}
			Padrao p = new Padrao(Pattern.compile(comps[0]),
					new Expectativa(TipoSchema.valueOf(comps[1]),Boolean.valueOf(comps[2]),
				Boolean.valueOf(comps[3]), Boolean.valueOf(comps[4])));
			logger.info("lido padrao: " + p);
			padroes.add(p);
		}
		br.close();
		return padroes;
	}
	
	public List<Exemplo> obtemExemplos() throws IOException {
		List<Padrao> padroes = obtemPadroes();
		List<Exemplo> l = new LinkedList<Exemplo>();
		InputStream is = ValidadorTest.class.getResourceAsStream("/" + listaExemplosFileName);
		if(is == null) {
			throw new RuntimeException("Não foi possível localizar recurso: " + listaExemplosFileName);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));		
		while(true) {
			String line = br.readLine();
			if(line == null) break;
			line = line.trim();
			if(line.isEmpty()) continue;
			boolean found = false;
			for(Padrao p : padroes) {
				Matcher m = p.pattern.matcher(line);
				if(m.matches()) {
					l.add(new Exemplo(p.expectativa,line));
					found = true;					
				} 
			}
			if(!found) {
				logger.warn("obtemExemplos: caminho " + line + " não bate com nenhum dos padrões.");
			}
		}
		br.close();
		return l;
	}

	private static void check(String t,boolean b,Collection<SAXParseException> c) {
		String msg = (b ? "D" : "Não d") + "evem haver " + t;
		boolean res = b ? !c.isEmpty() : c.isEmpty();
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		Iterator<SAXParseException> i = c.iterator();		
		if(i.hasNext()) {
			sb.append(i.next());
		}
		for(SAXParseException ex : c) {
			sb.append(", ");
			sb.append(ex.getMessage());
		}
		sb.append(']');
		assertTrue(msg + ": " + sb,res);
	}
	@Test
	public void testValidacao() throws ValidadorException, IllegalArgumentException, IOException {
		logger.info("testValidacao");				
		Validador validador = Validador.getInstance();
		List<Exemplo> exemplos = obtemExemplos();
		for(Exemplo exemplo : exemplos) {
			logger.info("vericando: " + exemplo);
			ResultadoValidacao res = 
				validador.valide(exemplo.expectativa.tipo,
						exemplo.path,ValidadorTest.class.getClassLoader());
			check("erros fatais",exemplo.expectativa.devemHaverErrosFatais,res.errosFatais);
			check("erros",exemplo.expectativa.devemHaverErros,res.erros);
			check("avisos",exemplo.expectativa.devemHaverAvisos,res.avisos);			
		}
	}		
}
