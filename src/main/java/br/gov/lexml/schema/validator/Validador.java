package br.gov.lexml.schema.validator;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

public class Validador {
	private static final Logger logger = LoggerFactory
			.getLogger(Validador.class);
	private static Validador instance = null;

	public static synchronized Validador getInstance()
			throws ValidadorException {
		if (instance == null) {
			instance = new Validador();
		}
		return instance;
	}

	private final XMLInputFactory inputFactory;
	private final XMLResolver resolver;
	private final LSResourceResolver resourceResolver;
	private final Map<TipoSchema, Schema> schemas = new HashMap<TipoSchema, Schema>();

	private void readSchema(SchemaFactory sf, TipoSchema tipo, String fileName) throws ValidadorException {
		logger.info("readSchema: fileName: " + fileName);
		try {
			InputStream is = getResourceAsStream(fileName);
			if(is == null) {				
				throw new ValidadorException("Nao foi possivel localizar recurso: " + fileName);
			}
			schemas.put(tipo, sf.newSchema(new StreamSource(is)));
		} catch (SAXException e) {
			throw new ValidadorException(
					"Erro durante a leitura do schema " + fileName, e);
		}
	}
	private Validador() throws ValidadorException {

		resolver = new MyXMLResolver();

		inputFactory = XMLInputFactory.newInstance();

		inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
		inputFactory.setProperty(
				XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
		inputFactory.setProperty(XMLInputFactory.RESOLVER, resolver);

		resourceResolver = new MyLSResourceResolver();

		SchemaFactory sf = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(resourceResolver);
		readSchema(sf,TipoSchema.RIGIDO,"xsd/lexml-br-rigido.xsd");
		readSchema(sf,TipoSchema.FLEXIVEL,"xsd/lexml-flexivel.xsd");
		readSchema(sf,TipoSchema.OAI,"xsd/oai_lexml.xsd");
		readSchema(sf,TipoSchema.EMENDA, "xsd/lexml-emenda-proposicao.xsd");		

	}

	private class MyXMLResolver implements XMLResolver {
		@Override
		public Object resolveEntity(String publicID, String systemID,
				String baseURI, String namespace) throws XMLStreamException {
			logger.info("MyXMLResolver.resolveEntity: resolving " + systemID);
			// Return null for default impl, or InputStream, or XMLStreamReader,
			// or XMLEventReader
			return getResourceAsStream(systemID);
		}
	}

	private static InputStream getResourceAsStream(String resource) {
		logger.info("Validador.getResourceAsStream: resorce = " + resource);
		ClassLoader cl = Validador.class.getClassLoader();
		if(cl instanceof URLClassLoader) {
			URLClassLoader ucl = (URLClassLoader) cl;
			logger.info("classloader urls: " + Arrays.asList(ucl.getURLs()));
		} else {
			logger.info("classloader class: " + cl.getClass().getName());
		}
		return cl.getResourceAsStream(resource);
	}

	private class MyLSResourceResolver implements LSResourceResolver {
		private final Logger logger = LoggerFactory
				.getLogger(MyLSResourceResolver.class);

		@Override
		public LSInput resolveResource(final String type,
				final String namespaceURI, final String publicId,
				final String systemId, final String baseURI) {
			logger.info("MyLSResolver.resolveEntity: resolving namespaceURI: " + namespaceURI + ", systemId: " + systemId + ", baseURI: " + baseURI);
			final String resourceName = resolveResourceName(namespaceURI,
					systemId, baseURI);
			if (resourceName == null) {
				logger.error("resolveResource: cannot resolve " + namespaceURI);
				return null;
			} else {
				logger.info("resolveResource: resolved " + namespaceURI
						+ " to " + resourceName);
			}
			// Return null for default impl or a LSInput
			return new LSInput() {

				@Override
				public Reader getCharacterStream() {
					logger.info("getCharacterStream");
					return new BufferedReader(new InputStreamReader(
							getResourceAsStream("xsd/" + resourceName)));
				}

				@Override
				public void setCharacterStream(Reader characterStream) {
					throw new RuntimeException(
							"unexpected method call: setCharacterStream");
				}

				@Override
				public InputStream getByteStream() {
					return null;
				}

				@Override
				public void setByteStream(InputStream byteStream) {
					throw new RuntimeException(
							"unexpected method call: setByteStream");
				}

				@Override
				public String getStringData() {
					return null;
				}

				@Override
				public void setStringData(String stringData) {
					throw new RuntimeException(
							"unexpected method call: setStringData");
				}

				@Override
				public String getSystemId() {
					return systemId;
				}

				@Override
				public void setSystemId(String systemId) {
					throw new RuntimeException(
							"unexpected method call: setSystemId");

				}

				@Override
				public String getPublicId() {
					return publicId;
				}

				@Override
				public void setPublicId(String publicId) {
					throw new RuntimeException(
							"unexpected method call: setPublicId");
				}

				@Override
				public String getBaseURI() {
					return baseURI;
				}

				@Override
				public void setBaseURI(String baseURI) {
					throw new RuntimeException(
							"unexpected method call: setBaseURI");
				}

				@Override
				public String getEncoding() {
					return "utf-8";
				}

				@Override
				public void setEncoding(String encoding) {
					throw new RuntimeException(
							"unexpected method call: setEncoding");
				}

				@Override
				public boolean getCertifiedText() {
					return false;
				}

				@Override
				public void setCertifiedText(boolean certifiedText) {
					throw new RuntimeException(
							"unexpected method call: setCertifiedText");
				}

			};
		}
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, Source source)
			throws IOException, IllegalArgumentException {
		ResultadoValidacao res = new ResultadoValidacao();
		Schema schema = schemas.get(tipoSchema);
		Validator validator = schema.newValidator();
		validator.setErrorHandler(res);
		validator.setResourceResolver(resourceResolver);
		try {
			validator.validate(source);
		} catch (SAXException e) {
			throw new RuntimeException("Unexpected exception", e);
		}
		return res;
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, Reader fonte)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, new StreamSource(fonte));
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, char[] fonte)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, new CharArrayReader(fonte));
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, String fonte)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, new StreamSource(new StringReader(fonte)));
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, InputStream fonte,
			String encoding) throws IOException, IllegalArgumentException {
		return valide(tipoSchema, new StreamSource(fonte, encoding));
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, InputStream fonte)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, fonte, "utf-8");
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, byte[] fonte,
			String encoding) throws IOException, IllegalArgumentException {
		return valide(tipoSchema, new ByteArrayInputStream(fonte), encoding);
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema, byte[] fonte)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, fonte, "utf-8");
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema,
			String resourceName, Class<?> clazz, String encoding)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, clazz.getResourceAsStream(resourceName),
				encoding);
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema,
			String resourceName, Class<?> clazz) throws IOException,
			IllegalArgumentException {
		return valide(tipoSchema, resourceName, clazz, "utf-8");
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema,
			String resourceName, ClassLoader loader, String encoding)
			throws IOException, IllegalArgumentException {
		return valide(tipoSchema, loader.getResourceAsStream(resourceName),
				encoding);
	}

	public ResultadoValidacao valide(TipoSchema tipoSchema,
			String resourceName, ClassLoader loader) throws IOException,
			IllegalArgumentException {
		return valide(tipoSchema, resourceName, loader, "utf-8");
	}

	private String resolveResourceName(String namespaceURI, String systemId,
			String baseURI) {
		final String res;
		if (baseURI != null
				&& baseURI
						.equals("http://www.w3.org/Math/XMLSchema/mathml2/mathml2.xsd")) {
			res = "mathml2/" + systemId;
		} else if (systemId != null && systemId.contains("mathml2")) {
			res = systemId.replaceFirst("^.*mathml2/", "mathml2/");
		} else if (systemId != null) {
			res = systemId.replaceFirst("^.*/", "");
		} else {
			res = null;
		}
		logger.info(String
				.format("resolverResourceName: namespaceURI = %s, systemId = %s, baseURI = %s, res = %s",
						namespaceURI, systemId, baseURI, res));
		return res;
	}
}
