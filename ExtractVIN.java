package com.openkm.automation.action;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.openkm.automation.Action;
import com.openkm.automation.AutomationUtils;
import com.openkm.bean.Document;
import com.openkm.dao.bean.Automation;
import com.openkm.util.FileUtils;
import com.openkm.util.PathUtils;
import com.openkm.api.OKMDocument;
import com.openkm.api.OKMFolder;

import net.xeoh.plugins.base.annotations.PluginImplementation;

/**
 * ExtractVIN This Action plugin reads a VIN (Vehicle Identification Number)
 * from a .txt file and splits it into WMI, VDS, and VIS on a new .txt file. An
 * output category/folder must be selected.
 */
@PluginImplementation
public class ExtractVIN implements Action {
	// private static Logger logger = LoggerFactory.getLogger(ExtractVIN.class);

	@Override
	public void executePre(Map<String, Object> env, Object... params) throws Exception {
	}

	@Override
	public void executePost(Map<String, Object> env, Object... params) throws Exception {
		// Input document to extract the VIN from.
		String inDocumentUUID = AutomationUtils.getUuid(env);

		// Output category to put the processed contents into.
		String outCategoryUUID = AutomationUtils.getString(0, params);

		String inDocumentPath = OKMDocument.getInstance().getPath(null, inDocumentUUID);
		String outCategoryPath = OKMFolder.getInstance().getPath(null, outCategoryUUID);

		if (inDocumentPath != null && outCategoryPath != null) {
			Document document = new Document();

			String inDocumentName = FileUtils.getFileName(PathUtils.getName(inDocumentPath));

			// Formatted output document name:
			// "{output_path}/{input_document}-out.txt"
			String outPath = String.format("%s/%s-out.txt", outCategoryPath, inDocumentName);

			document.setPath(outPath);

			OKMDocument.getInstance().getContent(null, inDocumentUUID, false);

			InputStream extract2 = OKMDocument.getInstance().getContent(null, inDocumentUUID, false);

			String extract = "";
			
			try (Scanner scanner = new Scanner(extract2)) {
				// Files are small, for now, so we're reading them all at once, and then trimming.
				extract = scanner.useDelimiter("\\A").next();
				
				extract = extract.replaceAll("[\\n\\r]", "").trim();
								
				// VIN pattern matching isn't perfect, but I wanted to include a little bit to show it could be better.
				if (extract != null && Pattern.matches("[(A-H|J-N|P|R-Z|0-9)]{17}", extract)) {
					// Lots of different ways to deal with this part. I chose this one because I like Maps and Streams.
					Map<String, String> VINComponents = new HashMap<>();

					VINComponents.put("WMI", extract.substring(0, 3));
					VINComponents.put("VDS", extract.substring(3, 9));
					VINComponents.put("VIS", extract.substring(9, 16));

					String result = VINComponents.entrySet().stream()
							.map((entry) -> entry.getKey() + " - " + entry.getValue()).collect(Collectors.joining("\n"));

					InputStream formattedInputStream = new ByteArrayInputStream(result.getBytes());

					document = OKMDocument.getInstance().create(null, document, formattedInputStream);
				}
			}		

		}
	}

	@Override
	public boolean hasPre() {
		return false;
	}

	@Override
	public boolean hasPost() {
		return true;
	}

	@Override
	public String getName() {
		return "ExtractVINs";
	}

	@Override
	public String getParamType00() {
		return Automation.PARAM_TYPE_TEXT;
	}

	@Override
	public String getParamSrc00() {
		return Automation.PARAM_SOURCE_FOLDER;
	}

	@Override
	public String getParamDesc00() {
		return "Out";
	}

	@Override
	public String getParamType01() {
		return Automation.PARAM_TYPE_EMPTY;
	}

	@Override
	public String getParamSrc01() {
		return Automation.PARAM_TYPE_EMPTY;
	}

	@Override
	public String getParamDesc01() {
		return "";
	}

	@Override
	public String getParamType02() {
		return Automation.PARAM_TYPE_EMPTY;
	}

	@Override
	public String getParamSrc02() {
		return Automation.PARAM_SOURCE_EMPTY;
	}

	@Override
	public String getParamDesc02() {
		return "";
	}
}