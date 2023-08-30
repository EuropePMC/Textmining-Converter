package uk.ac.ebi.literature.textminingapi.pojo;

public final class FileContent {

	private final byte[] content;
	private final String mimeType;
	private final long fileSizeInBytes;

	public FileContent(byte[] content, String mimeType, long fileSizeInBytes) {
		this.content = content;
		this.mimeType = mimeType;
		this.fileSizeInBytes = fileSizeInBytes;
	}

	public byte[] getContent() {
		return this.content;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public long getFileSizeInBytes() {
		return fileSizeInBytes;
	}

}
