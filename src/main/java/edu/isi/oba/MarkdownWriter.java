package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.Schema;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public final class MarkdownWriter {

	public static void writeMarkdownFile(YamlConfig configData, Mapper mapper) throws Exception {

		final var annotationConfig = configData.getAnnotation_config();
		if (annotationConfig.isPresent()) {
			final var tableOfContentsList = new ArrayList<String>();

			final var markdownFilename = annotationConfig.get().getMarkdown_generation_filename();

			if (markdownFilename != null) {
				final var destinationDir =
						configData.getOutput_dir() + File.separator + configData.getName();
				final var destinationProjectDirectory = destinationDir + File.separator;
				final var markdownFilePath = destinationProjectDirectory + markdownFilename;

				final var file = new File(markdownFilePath);
				BufferedWriter writer =
						Files.newBufferedWriter(
								file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
				writer.write("");

				final var markdownGenerationAnnotations =
						annotationConfig.get().getMarkdown_generation_annotations();
				if (markdownGenerationAnnotations.isPresent()) {
					markdownGenerationAnnotations
							.get()
							.forEach(
									(markdownGenerationAnnotationConfig) -> {
										final var currentMarkdownAnnotationName =
												markdownGenerationAnnotationConfig.getAnnotation_name();
										try {
											// For each annotation type, output its mapping details.
											final var annotationDetailsMap =
													mapper.getFullMarkdownMappings().get(currentMarkdownAnnotationName);
											if (annotationDetailsMap != null && !annotationDetailsMap.isEmpty()) {
												final var markdownHeading =
														markdownGenerationAnnotationConfig.getMarkdown_heading();
												if (markdownHeading == null || markdownHeading.isBlank()) {
													writer.append("# " + currentMarkdownAnnotationName);
												} else {
													writer.append("# " + markdownHeading);
												}

												writer.newLine();
												writer.newLine();

												final var markdownDescription =
														markdownGenerationAnnotationConfig.getMarkdown_description();
												if (markdownDescription != null && !markdownDescription.isBlank()) {
													writer.append(markdownDescription);
													writer.newLine();
												}

												writer.newLine();
												writer.append("[//]: # (INSERT TABLE OF CONTENTS HERE)");
												writer.newLine();

												insertMarkdownStyleBlock(writer);
												writer.newLine();
												writer.newLine();

												String currentClassKey = null;
												Schema currentClassSchema = null;
												for (final var entry : annotationDetailsMap.entrySet()) {
													final var key = entry.getKey();
													final var annotationValue = entry.getValue();

													final var lineSep = System.getProperty("line.separator");
													// Special "substitute character"
													final var specialTempChar = "\u001A";
													final var scrubbedAnnotationValue =
															annotationValue
																	.replaceAll("\r\n", specialTempChar)
																	.replaceAll("\r", specialTempChar)
																	.replaceAll("\n", specialTempChar)
																	.replaceAll("[" + specialTempChar + "]+", lineSep)
																	.replaceAll("<br />", lineSep + "\t\t<br />" + lineSep)
																	.replaceAll(lineSep + lineSep, lineSep + "<br />" + lineSep)
																	.replaceAll(lineSep, lineSep + "\t\t");

													final var entryKeyNameArray = key.split("#");
													final var entryClassName = entryKeyNameArray[0];
													final var entryPropertyName =
															entryKeyNameArray.length > 1 ? entryKeyNameArray[1] : null;
													String entryPropertyNamePlural = null;

													var keyReferenceId = key.replace("#", "-").toLowerCase();

													if (currentClassKey == null || !currentClassKey.equals(entryClassName)) {
														currentClassKey = entryClassName;

														writer.append("<hr class='class-separator' />");
														writer.newLine();
														writer.newLine();

														final var classHeaderLink =
																"<a id='" + currentClassKey.toLowerCase() + "'></a>";
														final var classHeaderLinkRef =
																"[" + currentClassKey + "](#" + currentClassKey.toLowerCase() + ")";
														tableOfContentsList.add("- " + classHeaderLinkRef);

														writer.append(classHeaderLink);
														writer.newLine();
														writer.append("## " + currentClassKey);

														currentClassSchema = mapper.getSchemas().get(currentClassKey);

														if (currentClassSchema != null) {
															writer.newLine();
															writer.newLine();

															var classDescription = currentClassSchema.getDescription();
															classDescription =
																	classDescription == null
																			? "<span class='italic'>No description exists within the"
																					+ " ontology.  Please update!</span>"
																			: classDescription;
															writer.append(classDescription);

															if (key.equals(entryClassName)
																	&& (currentClassSchema.getEnum() == null
																			|| currentClassSchema.getEnum().isEmpty())) {
																writer.newLine();
																writer.append("<div class='class-grid'>");
																writer.newLine();
																writer.append("\t<div class='content'>");
																writer.newLine();
																writer.append("\t\t" + scrubbedAnnotationValue);
																writer.newLine();
																writer.append("\t</div>");
																writer.newLine();
																writer.append("</div>");
																writer.newLine();
															}
														}

														final var allOfList = currentClassSchema.getAllOf();
														if (allOfList != null) {
															writer.newLine();
															writer.newLine();

															final var classInheritedPropsLink =
																	"<a id='inherited-properties-for-"
																			+ entryClassName.toLowerCase()
																			+ "'></a>";
															final var classInheritedProps =
																	"[Inherited properties](#inherited-properties-for-"
																			+ entryClassName.toLowerCase()
																			+ ")";
															tableOfContentsList.add("\t- " + classInheritedProps);

															writer.append(classInheritedPropsLink);
															writer.newLine();
															writer.append("### Inherited properties");

															writer.newLine();
															writer.append("<ul>");
															writer.newLine();
															for (final var allOfItem : allOfList) {
																final var allOfRef = ((Schema) allOfItem).get$ref();
																if (allOfRef != null) {
																	final var refClassName =
																			allOfRef.replace("#/components/schemas/", "");
																	writer.append(
																			"\t<li><a href='#"
																					+ refClassName.toLowerCase()
																					+ "'>"
																					+ refClassName
																					+ "</a></li>");
																	writer.newLine();
																}
															}
															writer.append("</ul>");
														}

														if (currentClassSchema.getProperties() != null
																&& !currentClassSchema.getProperties().isEmpty()) {
															writer.newLine();
															writer.newLine();

															final var classSpecificPropsLink =
																	"<a id='class-specific-properties-for-"
																			+ entryClassName.toLowerCase()
																			+ "'></a>";
															final var classSpecificProps =
																	"[Class-specific properties](#class-specific-properties-for-"
																			+ entryClassName.toLowerCase()
																			+ ")";
															tableOfContentsList.add("\t- " + classSpecificProps);

															writer.append(classSpecificPropsLink);
															writer.newLine();
															writer.append("### Class-specific properties");
														}

														writer.newLine();
														writer.newLine();
													}

													// If the current property name is not found in the list of properties
													// (and it's not an enum), then lets assume the property is plural
													// instead.
													if (currentClassSchema != null
															&& (currentClassSchema.getEnum() == null
																	|| currentClassSchema.getEnum().isEmpty())
															&& (currentClassSchema.getProperties() == null
																	|| !currentClassSchema
																			.getProperties()
																			.containsKey(entryPropertyName))) {
														entryPropertyNamePlural = ObaUtils.getPluralOf(entryPropertyName);
														keyReferenceId =
																key.replace("#", "-")
																		.replace("-" + entryPropertyName, "-" + entryPropertyNamePlural)
																		.toLowerCase();
													}

													// If the class is an enum, then don't treat it as having properties.
													final var enumList =
															currentClassSchema == null ? null : currentClassSchema.getEnum();
													if (enumList != null) {
														writer.append("<div class='prop-grid'>");
														writer.newLine();
														writer.append("\t<div class='header'>+_Ontology_Details_+</div>");
														writer.newLine();
														writer.append("\t<div class='header'>+_Mapped_To_Details_+</div>");
														writer.newLine();
														writer.append("\t<div class='content'>");
														writer.newLine();
														writer.append("\t\t<div class='italic'>enum values:</div>");
														writer.newLine();
														writer.append("\t\t<div class='data_property_info'>");
														writer.newLine();
														writer.append("\t\t\t<ul class='enums'>");
														writer.newLine();

														for (final var enumItem : enumList) {
															writer.append(
																	"\t\t\t\t<li>" + enumItem.toString().toLowerCase() + "</li>");
															writer.newLine();
														}

														writer.append("\t\t\t</ul>");
														writer.newLine();
														writer.append("\t\t</div>");
														writer.newLine();
														writer.append("\t</div>");
													} else if (entryPropertyName != null) {
														final var classProp =
																(entryPropertyNamePlural == null
																		? entryPropertyName
																		: entryPropertyNamePlural);
														final var classPropLink = "<a id='" + keyReferenceId + "'></a>";
														final var classPropLinkRef =
																"[" + classProp + "](#" + keyReferenceId + ")";
														tableOfContentsList.add("\t\t- " + classPropLinkRef);

														writer.append(classPropLink);
														writer.newLine();
														writer.append("#### " + classProp);

														writer.newLine();

														if (currentClassSchema != null) {
															final var propertiesMap = currentClassSchema.getProperties();
															final var propertySchema =
																	propertiesMap == null
																			? null
																			: (Schema)
																					propertiesMap.get(
																							entryPropertyNamePlural == null
																									? entryPropertyName
																									: entryPropertyNamePlural);

															if (propertySchema != null) {
																writer.newLine();

																var isWriteOnlyObjProp = false;
																var isReadOnlyObjProp = false;

																if (propertySchema.getDescription() != null
																		&& !propertySchema.getDescription().isBlank()) {
																	writer.append(propertySchema.getDescription());
																	writer.newLine();
																	writer.newLine();
																} else if (propertySchema.getAllOf() != null) {
																	for (final var allOfItem : propertySchema.getAllOf()) {
																		final var allOfItemSchema = (Schema) allOfItem;
																		if (allOfItemSchema != null) {
																			if (allOfItemSchema.getDescription() != null
																					&& !allOfItemSchema.getDescription().isBlank()) {
																				writer.append(allOfItemSchema.getDescription());
																				writer.newLine();
																				writer.newLine();
																			}

																			if (allOfItemSchema.getWriteOnly() != null) {
																				isWriteOnlyObjProp = allOfItemSchema.getWriteOnly();
																			} else if (allOfItemSchema.getReadOnly() != null) {
																				isReadOnlyObjProp = allOfItemSchema.getReadOnly();
																			}
																		}
																	}
																}

																final var isWriteOnlyDataProp =
																		propertySchema.getWriteOnly() != null
																				&& propertySchema.getWriteOnly();
																final var isReadOnlyDataProp =
																		propertySchema.getReadOnly() != null
																				&& propertySchema.getReadOnly();

																if (isWriteOnlyDataProp || isWriteOnlyObjProp) {
																	writer.append(
																			":warning:<span style='font-weight: bold; background-color:"
																					+ " yellow;'><span style='font-variant:"
																					+ " small-caps;'>post/put</span> operations <span"
																					+ " class='italic'>only</span>!</span>");
																	writer.newLine();
																	writer.newLine();
																} else if (isReadOnlyDataProp || isReadOnlyObjProp) {
																	writer.append(
																			":warning:<span style='font-weight: bold; background-color:"
																					+ " yellow;'><span style='font-variant:"
																					+ " small-caps;'>get</span> operations <span"
																					+ " class='italic'>only</span>!</span>");
																	writer.newLine();
																	writer.newLine();
																}

																writer.append("<div class='prop-grid'>");
																writer.newLine();
																writer.append("\t<div class='header'>+_Ontology_Details_+</div>");
																writer.newLine();
																writer.append("\t<div class='header'>+_Mapped_To_Details_+</div>");
																writer.newLine();
																writer.append("\t<div class='content'>");

																final var requiredProps = currentClassSchema.getRequired();
																if (requiredProps != null
																		&& requiredProps.contains(
																				entryPropertyNamePlural == null
																						? entryPropertyName
																						: entryPropertyNamePlural)) {
																	writer.newLine();
																	writer.append(
																			"\t\t<pre class='required symbol'>&#x002A;</pre>&nbsp;<span"
																					+ " class='required'>required</span>");
																} else {
																	writer.newLine();
																	writer.append(
																			"\t\t<pre class='optional symbol'>&#x003F;</pre>&nbsp;<span"
																					+ " class='optional'>optional</span>");
																}

																writer.newLine();
																writer.append("\t\t<br />");
																writer.newLine();

																if (propertySchema.getNullable() != null) {
																	if (propertySchema.getNullable()) {
																		writer.append(
																				"\t\t<pre class='nullable symbol'>&#x2205;</pre>&nbsp;<span"
																						+ " class='nullable'>nullable</span>");
																	} else {
																		writer.append(
																				"\t\t<pre class='notnullable"
																						+ " symbol'>&#x0021;&#x2205;</pre>&nbsp;<span"
																						+ " class='notnullable'>not nullable</span>");
																	}
																} else {
																	writer.append(
																			"\t\t<pre class='nullable symbol'>&#x2205;</pre>&nbsp;<span"
																					+ " class='nullable'>nullable</span>");
																}

																writer.newLine();
																writer.append("\t\t<div class='spacer'></div>");
																writer.newLine();

																// The type/format/pattern for object properties is null
																if (propertySchema.getType() != null) {
																	writer.append(
																			"\t\t<div class='italic'>datatype information:</div>");
																	writer.newLine();
																	writer.append("\t\t<div class='data_property_info'>");
																	writer.newLine();
																	writer.append("\t\t\t<ul>");
																	writer.newLine();

																	if ("array".equals(propertySchema.getType())) {
																		final var propertySchemaItems = propertySchema.getItems();
																		if (propertySchemaItems.get$ref() == null) {
																			writer.append(
																					"\t\t\t\t<li><span class='type_info'>type:</span><span>"
																							+ propertySchemaItems.getType()
																							+ "</span></li>");
																		} else {
																			final var objPropRef =
																					propertySchemaItems
																							.get$ref()
																							.replace("#/components/schemas/", "");
																			final var markdownRef =
																					"<a href='#"
																							+ objPropRef.toLowerCase()
																							+ "'>"
																							+ objPropRef
																							+ "</a>";
																			writer.append(
																					"\t\t\t\t<li><span class='type_info'>type:</span><span>"
																							+ propertySchema.getType()
																							+ " of:<br />"
																							+ markdownRef
																							+ "</span></li>");
																		}
																	} else {
																		writer.append(
																				"\t\t\t\t<li><span class='type_info'>type:</span><span>"
																						+ propertySchema.getType()
																						+ "</span></li>");
																	}

																	if (propertySchema.getFormat() != null
																			&& !propertySchema.getFormat().isBlank()) {
																		writer.newLine();
																		writer.append(
																				"\t\t\t\t<li><span class='type_info'>format:</span>"
																						+ propertySchema.getFormat()
																						+ "</li>");
																	}

																	if (propertySchema.getPattern() != null
																			&& !propertySchema.getPattern().isBlank()) {
																		writer.newLine();
																		writer.append(
																				"\t\t\t\t<li><span class='type_info'>pattern:</span>"
																						+ propertySchema.getPattern()
																						+ "</li>");
																	}

																	writer.newLine();
																	writer.append("\t\t\t</ul>");
																	writer.newLine();
																	writer.append("\t\t</div>");
																} else {
																	writer.append(
																			"\t\t<div class='italic'>nested object, see mapping:</div>");
																	writer.newLine();
																	writer.append("\t\t<div class='object_property_info'>");
																	writer.newLine();

																	final var temp =
																			(entryPropertyNamePlural == null
																							? entryPropertyName
																							: entryPropertyNamePlural)
																					.replaceAll("^has", "");
																	final var tempLower = temp.toLowerCase();
																	writer.append(
																			"\t\t\t<a href='#" + tempLower + "'>" + temp + "</a>");
																	writer.newLine();
																	writer.append("\t\t</div>");
																}

																writer.newLine();
																writer.append("\t</div>");
															}
														}
													}

													if (!key.equals(entryClassName)
															|| (currentClassSchema.getEnum() != null
																	&& !currentClassSchema.getEnum().isEmpty())) {
														writer.newLine();
														writer.append("\t<div class='content'>");
														writer.newLine();
														writer.append("\t\t" + scrubbedAnnotationValue);
														writer.newLine();
														writer.append("\t</div>");
														writer.newLine();
														writer.append("</div>");
														writer.newLine();
														writer.newLine();
														writer.newLine();
													}

													writer.flush();
												}

												currentClassKey = null;
												currentClassSchema = null;
											}
										} catch (Exception e) {
											logger.severe("Problem writing to markdown file: " + e.getLocalizedMessage());
											e.printStackTrace();
										}
									});
				}

				writer.append("<hr class='class-separator' style='margin-bottom: 0px;' />");
				writer.newLine();
				writer.append(
						"<div style='margin: 0px; width: 100%; text-align: center;'>End of document</div>");
				writer.newLine();
				writer.append("<hr class='class-separator' style='margin-top: 0px;' />");
				writer.newLine();
				writer.newLine();
				writer.flush();
				writer.close();

				// Do the TOC here!!!
				final var lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
				final var tocIndex = lines.indexOf("[//]: # (INSERT TABLE OF CONTENTS HERE)");
				if (tocIndex > -1) {
					lines.remove(tocIndex);
					tableOfContentsList.add(0, "## Table of contents");
					tableOfContentsList.add(1, "");
					lines.addAll(tocIndex, tableOfContentsList);
					Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
				}
			} else {
				logger.info("No markdown filename found.  Not writing markdown file.");
			}

		} else {
			logger.info("No annotation configuration found.  Not writing markdown file.");
		}
	}

	private static void insertMarkdownStyleBlock(BufferedWriter writer) throws Exception {
		writer.newLine();
		writer.append("<style>");
		writer.newLine();
		writer.append("\t.italic {");
		writer.newLine();
		writer.append("\t\tfont-style: italic;");
		writer.newLine();
		writer.append("\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t.class-separator {");
		writer.newLine();
		writer.append("\t\twidth: 75%;");
		writer.newLine();
		writer.append("\t\theight: 1px;");
		writer.newLine();
		writer.append("\t\tmargin: 15px auto -10px auto;");
		writer.newLine();
		writer.append(
				"\t\tbackground: -webkit-gradient(linear, 0 0, 100% 0, from(transparent),"
						+ " to(transparent), color-stop(50%, silver));");
		writer.newLine();
		writer.append("\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t.class-grid, .prop-grid {");
		writer.newLine();
		writer.append("\t\twidth: 80%;");
		writer.newLine();
		writer.append("\t\tmargin: 0px auto 25px auto;");
		writer.newLine();
		writer.append("\t\tdisplay: grid;");
		writer.newLine();
		writer.append("\t\tgrid-template-columns: 1fr 1fr;");
		writer.newLine();
		writer.append("\t\tpadding: 5px;");
		writer.newLine();
		writer.append("\t\tgap: 2px;");
		writer.newLine();
		writer.append("\t\tbackground-color: silver;");
		writer.newLine();
		writer.newLine();
		writer.append("\t\tdiv {");
		writer.newLine();
		writer.append("\t\t\tpadding: 3px;");
		writer.newLine();
		writer.append("\t\t\tbackground-color: ghostwhite;");
		writer.newLine();
		writer.append("\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t.header {");
		writer.newLine();
		writer.append("\t\t\tfont-weight: bold;");
		writer.newLine();
		writer.append("\t\t\ttext-align: center;");
		writer.newLine();
		writer.append("\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t.content {");
		writer.newLine();
		writer.append("\t\t\tpadding-left: 10px;");
		writer.newLine();
		writer.append("\t\t\tpadding-right: 10px;");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t.db-name-info {");
		writer.newLine();
		writer.append("\t\t\t\tfont-family: monospace;");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t\t.db-user-defined-datatype-info {");
		writer.newLine();
		writer.append("\t\t\t\t\tfont-style: italic;");
		writer.newLine();
		writer.append("\t\t\t\t\tfont-size: 65%;");
		writer.newLine();
		writer.append("\t\t\t\t}");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\tpre.symbol {");
		writer.newLine();
		writer.append("\t\t\t\twidth: 30px;");
		writer.newLine();
		writer.append("\t\t\t\tdisplay: inline-block;");
		writer.newLine();
		writer.append("\t\t\t\tmargin: 0px;");
		writer.newLine();
		writer.append("\t\t\t\tpadding: 0px;");
		writer.newLine();
		writer.append("\t\t\t\tvertical-align: text-top;");
		writer.newLine();
		writer.append("\t\t\t\tfont-style: normal;");
		writer.newLine();
		writer.append("\t\t\t\ttext-align: center;");
		writer.newLine();
		writer.append("\t\t\t\tbackground: none;");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t.optional, .nullable {");
		writer.newLine();
		writer.append("\t\t\t\tfont-style: italic;");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t.required, .notnullable {");
		writer.newLine();
		writer.append("\t\t\t\tcolor: crimson;");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t.spacer {");
		writer.newLine();
		writer.append("\t\t\t\theight: 6px;");
		writer.newLine();
		writer.append("\t\t\t\twidth: 100%;");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t.object_property_info {");
		writer.newLine();
		writer.append("\t\t\t\tmargin-left: 30px;");
		writer.newLine();
		writer.append("\t\t\t\tpadding-top: 0px;");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t.data_property_info {");
		writer.newLine();
		writer.append("\t\t\t\tmargin: 0px;");
		writer.newLine();
		writer.append("\t\t\t\tpadding-top: 0px;");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t\tul {");
		writer.newLine();
		writer.append("\t\t\t\t\tmargin-bottom: 0px;");
		writer.newLine();
		writer.append("\t\t\t\t\tpadding-left: 15px;");
		writer.newLine();
		writer.append("\t\t\t\t\tlist-style-position: outside;");
		writer.newLine();
		writer.append("\t\t\t\t\tlist-style-type: \"•\";");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t\t\tli {");
		writer.newLine();
		writer.append("\t\t\t\t\t\tpadding-inline-start: 15px;");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t\t\t\t.type_info {");
		writer.newLine();
		writer.append("\t\t\t\t\t\t\twidth: 35%;");
		writer.newLine();
		writer.append("\t\t\t\t\t\t\tfont-variant: small-caps;");
		writer.newLine();
		writer.append("\t\t\t\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t\t\t\tspan {");
		writer.newLine();
		writer.append("\t\t\t\t\t\t\tdisplay: inline-block;");
		writer.newLine();
		writer.append("\t\t\t\t\t\t\tmargin-top: -5px;");
		writer.newLine();
		writer.append("\t\t\t\t\t\t\tvertical-align: text-top;");
		writer.newLine();
		writer.append("\t\t\t\t\t\t}");
		writer.newLine();
		writer.append("\t\t\t\t\t}");
		writer.newLine();
		writer.append("\t\t\t\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t\t\t\tul.enums li {");
		writer.newLine();
		writer.append("\t\t\t\t\tfont-variant: small-caps;");
		writer.newLine();
		writer.append("\t\t\t\t}");
		writer.newLine();
		writer.append("\t\t\t}");
		writer.newLine();
		writer.append("\t\t}");
		writer.newLine();
		writer.append("\t}");
		writer.newLine();
		writer.newLine();
		writer.append("\t.class-grid {");
		writer.newLine();
		writer.append("\t\tgrid-template-columns: 1fr;");
		writer.newLine();
		writer.append("\t}");
		writer.newLine();
		writer.append("</style>");
		writer.newLine();
	}
}
