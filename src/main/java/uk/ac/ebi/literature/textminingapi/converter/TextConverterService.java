package uk.ac.ebi.literature.textminingapi.converter;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TextConverterService {

	private static final Logger log = LoggerFactory.getLogger(TextConverterService.class);

	public Optional<String> convert(byte[] content) {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
			BodyContentHandler handler = new BodyContentHandler(-1);
			AutoDetectParser parser = new AutoDetectParser();
			Metadata metadata = new Metadata();
			parser.parse(bais, handler, metadata);
			String textContent = handler.toString();
			log.info("Converted to text successfully!!!");
			return Optional.of(textContent);
		} catch (Exception e) {
			log.error("Exception while converting doc to text", e);
		}
		return Optional.empty();
	}
}
