package uk.ac.ebi.literature.textminingapi.utils;

import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.literature.textminingapi.pojo.FileContent;

public class ConverterUtils {

	private static final Logger log = LoggerFactory.getLogger(ConverterUtils.class);

	public static final String MIME_TYPE_UNDETERMINED = "Undetermined";

	public static FileContent getFileContent(String fileUrl) throws Exception {
		Metadata metadata = new Metadata();
		metadata.set(Metadata.RESOURCE_NAME_KEY, fileUrl);
		try (TikaInputStream tip = TikaInputStream.get(new URL(fileUrl))) {
			byte[] content = IOUtils.toByteArray(tip);
			TikaConfig tika = new TikaConfig();
			String mimeType = tika.getDetector().detect(tip, metadata).toString();
			log.info("mimeType=" + mimeType);
			return new FileContent(content, mimeType, content.length);
		}
	}

}
