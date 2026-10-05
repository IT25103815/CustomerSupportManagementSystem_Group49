package com.group49.support.util;

import jakarta.servlet.http.Part;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Validate actual bytes, never the client-supplied MIME header. */
public final class AttachmentValidator {
    public static final int MAX_FILES = Math.max(1, Math.min(3, Integer.getInteger("helpify.upload.files", 3)));
    public static final long MAX_BYTES = Math.max(1, Math.min(10 * 1024 * 1024L, Long.getLong("helpify.upload.bytes", 10 * 1024 * 1024L)));
    public record Upload(String name, String extension, String type, byte[] bytes) {}
    public static Upload validate(Part part) throws IOException {
        if (part.getSize() == 0 || part.getSize() > MAX_BYTES) throw new IllegalArgumentException("Files must be non-empty and at most " + MAX_BYTES / 1024 / 1024 + " MB.");
        String name = Optional.ofNullable(part.getSubmittedFileName()).orElse("").replace('\\','/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N} ._()-]", "_").trim();
        if (name.length() > 220) name = name.substring(name.length()-220);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        byte[] bytes;
        try (InputStream in = part.getInputStream()) { bytes = in.readNBytes((int)MAX_BYTES+1); }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("File exceeds the upload limit.");
        String type;
        if (Set.of(".png", ".jpg", ".jpeg").contains(ext)) {
            boolean png = starts(bytes, new byte[]{(byte)137,80,78,71,13,10,26,10});
            boolean jpeg = starts(bytes, new byte[]{(byte)255,(byte)216,(byte)255});
            if ((ext.equals(".png") ? !png : !jpeg)) throw new IllegalArgumentException("Image extension does not match its content.");
            try (var imageIn = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(imageIn);
                if (!readers.hasNext()) throw new IllegalArgumentException("Invalid image.");
                var reader = readers.next();
                try { reader.setInput(imageIn); if ((long)reader.getWidth(0)*reader.getHeight(0)>25000000L) throw new IllegalArgumentException("Image resolution is too large.");
                    if(reader.read(0)==null) throw new IllegalArgumentException("Invalid image.");
                } finally { reader.dispose(); }
            }
            type = png ? "image/png" : "image/jpeg";
        } else if (ext.equals(".pdf")) {
            String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (!text.startsWith("%PDF-") || !text.contains("%%EOF")) throw new IllegalArgumentException("Invalid PDF document.");
            // Parser validates the document structure, including its cross-reference data.
            try (var pdf = org.apache.pdfbox.pdmodel.PDDocument.load(bytes)) {
                if (pdf.getNumberOfPages() < 1) throw new IllegalArgumentException("PDF has no pages.");
            }
            type = "application/pdf";
        } else if (ext.equals(".docx")) {
            try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(bytes))) {
                if(doc.getDocument().getBody()==null) throw new IllegalArgumentException("Invalid Word document.");
            } catch (Exception e) { throw new IllegalArgumentException("Invalid DOCX document."); }
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (ext.equals(".doc")) {
            try (var doc = new org.apache.poi.hwpf.HWPFDocument(new ByteArrayInputStream(bytes))) { doc.getRange().text(); }
            catch (Exception e) { throw new IllegalArgumentException("Invalid DOC document."); }
            type = "application/msword";
        } else throw new IllegalArgumentException("Allowed files: PNG, JPG, JPEG, PDF, DOC and DOCX.");
        return new Upload(name, ext, type, bytes);
    }
    private static boolean starts(byte[] bytes, byte[] prefix) {
        if(bytes.length < prefix.length) return false;
        for(int i=0;i<prefix.length;i++) if(bytes[i]!=prefix[i]) return false;
        return true;
    }
}
