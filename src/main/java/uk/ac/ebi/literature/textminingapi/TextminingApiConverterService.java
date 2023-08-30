package uk.ac.ebi.literature.textminingapi;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.ac.ebi.literature.textminingapi.converter.TextConverterService;
import uk.ac.ebi.literature.textminingapi.pojo.Components;
import uk.ac.ebi.literature.textminingapi.pojo.FileContent;
import uk.ac.ebi.literature.textminingapi.pojo.MLTextObject;
import uk.ac.ebi.literature.textminingapi.service.FileService;
import uk.ac.ebi.literature.textminingapi.service.MLQueueSenderService;
import uk.ac.ebi.literature.textminingapi.utility.Utility;
import uk.ac.ebi.literature.textminingapi.utils.ConverterUtils;

@Service
public class TextminingApiConverterService {

	private static final Logger log = LoggerFactory.getLogger(TextminingApiConverterService.class);

	@Value("${rabbitmq.tmExchange}")
	private String textminingExchange;

	@Value("${rabbitmq.outcomeQueue}")
	private String textminingOutcomeQueue;

	@Value("${rabbitmq.plaintextQueue}")
	private String textminingPlainTextQueue;

	@Value("#{'${converter.allowedMimeTypes}'.split(',')}")
	private Set<String> allowedMimeTypes;

	@Value("${converter.allowedFileSizeInMB}")
	private int allowedFileSizeInMB;

	private final MLQueueSenderService mlQueueSenderService;

	private final FileService fileService;

	private final TextConverterService textConverterService;

	public TextminingApiConverterService(MLQueueSenderService mlQueueSenderService, FileService fileService, TextConverterService textConverterService) {
		this.mlQueueSenderService = mlQueueSenderService;
		this.fileService = fileService;
		this.textConverterService = textConverterService;
	}

	@RabbitListener(autoStartup = "${rabbitmq.submissionsQueue.autoStartUp}", queues = "${rabbitmq.submissionsQueue}")
	public void listenForMessage(Message message) throws Exception {
		MLTextObject mlTextObject = Utility.castMessage(message, MLTextObject.class);
		if (mlQueueSenderService.hasExceededRetryCount(message)) {
			log.info("{" + mlTextObject.getFtId() + "} retry count exceeded");
			Utility.markMessageAsFailed(mlTextObject, Components.FETCHER);
			mlQueueSenderService.sendMessageToQueue(textminingOutcomeQueue, mlTextObject, textminingExchange);
		} else {
			log.info("{" + mlTextObject.getFtId() + "} listening...");
			String url = mlTextObject.getUrl();

			log.info("{" + mlTextObject.getFtId() + "}, url {" + url + "}");

			FileContent fileContent = ConverterUtils.getFileContent(url);

			log.info("{" + mlTextObject.getFtId() + "} content retrieved successfully for {"
					+ mlTextObject.getFilename() + "}");
			log.info("{" + mlTextObject.getFtId() + "} mimeType is {" + fileContent.getMimeType() + "}");

			long allowedFileSizeInBytes = this.allowedFileSizeInMB * 1024L * 1024L;

			log.info("{" + mlTextObject.getFtId() + "} File Size is {" + fileContent.getFileSizeInBytes() + "}");

			if (fileContent.getFileSizeInBytes() > allowedFileSizeInBytes) {
				log.info("{" + mlTextObject.getFtId() + "} File size limit exceeds for {" + mlTextObject.getFilename()
						+ "}");
				Utility.markMessageAsFailed(mlTextObject, Components.CONVERTER, "File size should be <= "
						+ (this.allowedFileSizeInMB) + "MB for " + mlTextObject.getFilename());
				mlQueueSenderService.sendMessageToQueue(textminingOutcomeQueue, mlTextObject, textminingExchange);
				return;
			}
			log.info("{" + mlTextObject.getFtId() + "} File Size in Limit");
			if (!allowedMimeTypes.contains(fileContent.getMimeType())) {
				log.info("{" + mlTextObject.getFtId() + "} mimeType not allowed for {" + mlTextObject.getFilename()
						+ "}");
				Utility.markMessageAsFailed(mlTextObject, Components.CONVERTER, "Mimetype " + fileContent.getMimeType()
						+ " not allowed for file " + mlTextObject.getFilename());
				mlQueueSenderService.sendMessageToQueue(textminingOutcomeQueue, mlTextObject, textminingExchange);
				return;
			}
			log.info("{" + mlTextObject.getFtId() + "} MimeType is Allowed");
			Optional<String> optionalTextContent = textConverterService.convert(fileContent.getContent());
			log.info("{" + mlTextObject.getFtId() + "} Conversion Done");
			if (optionalTextContent.isEmpty() || StringUtils.isBlank(optionalTextContent.get())) {
				log.info("{" + mlTextObject.getFtId() + "} conversion to text failed for {" + mlTextObject.getFilename()
						+ "}");
				Utility.markMessageAsFailed(mlTextObject, Components.CONVERTER,
						"Failure in converting the file to text for file " + mlTextObject.getFilename());
				mlQueueSenderService.sendMessageToQueue(textminingOutcomeQueue, mlTextObject, textminingExchange);
				return;
			}
			log.info("{" + mlTextObject.getFtId() + "} Going to create text file");
			String fileName = new StringBuilder(mlTextObject.getUser()).append("_").append(mlTextObject.getFtId()).append("_").append(mlTextObject.getFilename()).append("_text_version.txt").toString();

			String path = fileService.write(optionalTextContent.get(), fileName);

			log.info("{" + mlTextObject.getFtId() + "} converted to text successfully with path {" + path + "}");

			mlTextObject.setProcessingFilename(fileName);

			boolean sending = mlQueueSenderService.sendMessageToQueue(textminingPlainTextQueue, mlTextObject,
					textminingExchange);
			if (sending == false) {
				throw new Exception("Impossible to store Success in PlainText queue  for " + mlTextObject.toString());
			}
			log.info("{" + mlTextObject.getFtId() + "} successfully processed? {" + sending + "}");
		}
	}
}
