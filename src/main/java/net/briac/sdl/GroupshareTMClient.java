/**************************************************************************
SDLApiClient - GroupShare Web API

Copyright (C) 2019 Briac Pilpré
Home page: http://www.omegat.org/
Support center: http://groups.yahoo.com/group/OmegaT/

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
**************************************************************************/
package net.briac.sdl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JTextArea;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.briac.sdl.model.Child;
import net.briac.sdl.model.File;
import net.briac.sdl.model.Organization;
import net.briac.sdl.model.OrganizationResource;
import net.briac.sdl.model.ParagraphUnit;
import net.briac.sdl.model.SystemFields;
import net.briac.sdl.model.TranslationUnits;


public class GroupshareTMClient extends DefaultHandler {

    private String sourceLanguage;
    private String targetLanguage;
    private final List<String> providerUri = new ArrayList<>();
    private String orgPath;
    private String tmName;
    private JTextArea logArea;

    public static void main(String[] args) throws Exception {
        GroupshareTMClient act = new GroupshareTMClient();

        if (args.length < 4) {
            GroupshareTMWindow win = new GroupshareTMWindow();
            win.setVisible(true);
        } else {

            if (args.length < 4) {
                System.out.println("GroupshareTMClient sdlServer username password pptxFile");
                System.exit(1);
            }

            String sdlServer = args[0];
            String username = args[1];
            String password = args[2];
            String sdlppxFile = args[3];

            System.out.println("SDL Server: [" + sdlServer + "]");
            System.out.println("Username  : [" + username + "]");
            System.out.println("Password  : [******]");
            System.out.println("SDLPPX    : [" + sdlppxFile + "]");
            System.out.println("");

            act.downloadTM(sdlServer, username, password, sdlppxFile);
        }
    }

    public void downloadTM(String sdlServer, String username, String password, String sdlppxFile) throws Exception {

        String outputDirectory = new java.io.File(sdlppxFile).getParent();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        ZipFile zipFile = new ZipFile(new java.io.File(sdlppxFile));
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".sdlproj")) {
                // log(entry.getName());
                parser.parse(zipFile.getInputStream(entry), this);
            }
        }
        zipFile.close();
        if (providerUri == null || providerUri.isEmpty()) {
            log("No MainTranslationProviderItem found, no TMX to generate");
            return;
        }

        log("sourceLanguage: " + sourceLanguage);
        log("targetLanguage: " + targetLanguage);

        for (String pu : providerUri) {

            URI uri = new URI(pu);

            String query = uri.getRawQuery();
            // Query decoded for each param individually, since some param value may contain
            // "&" inside (e.g. "&amp;")
            for (String queryPart : query.split("&")) {
                String[] kv = queryPart.split("=", 2);
                if (kv[0].equals("orgPath")) {
                    orgPath = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.toString()).replaceFirst("/", "");
                } else if (kv[0].equals("tmName")) {
                    tmName = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.toString());
                }
            }

            log("=====================================================================");
            log("company       : " + orgPath);
            log("tmName        : " + tmName);
            log("providerUri   : " + pu);

            java.io.File tmxFile = new java.io.File(outputDirectory, tmName + ".tmx");

            log("output TMX    : " + tmxFile.getAbsolutePath());

            // http://gs2017dev.sdl.com:41235/docs/ui/index#!/Translation_Units/TranslationUnits_PostGetTusWithIterator
            SDLApiClient app = new SDLApiClient(sdlServer, username, password);
            app.login();

            Map<String, Organization> orgs = app.getOrganizations();
            Organization org = orgs.get(orgPath);

            if (org == null) {
                log("No organization \"" + orgPath + "\", aborting.");
                log("Know organisations:");
                for (String orgName : orgs.keySet()) {
                    log("  - " + orgName);
                }
                return;
            }

            Map<String, OrganizationResource> resources = app.getOrganizationResources(org);
            OrganizationResource orgRes = resources.get(tmName);
            if (orgRes == null) {
                log("No resource \"" + tmName + "\", aborting.");
                log("Know resources:");
                for (String resName : resources.keySet()) {
                    log("  - " + resName);
                }
                return;
            }

            // log("TM ID: " + orgRes); System.exit(23);
            getTMX(app, sourceLanguage, targetLanguage, orgRes, tmxFile);

            log("TMX saved.");
        }

        int size = providerUri.size();
        log("Operation done, " + size + " file" + (size > 1 ? "s" : "") + " saved.");
    }

    private void getTMX(SDLApiClient app, String source, String target, OrganizationResource tmResource,
            java.io.File outputFile) throws FileNotFoundException, XMLStreamException, FactoryConfigurationError,
            UnsupportedEncodingException, IOException, JsonParseException, JsonMappingException {

        int tuCount = app.getTuCount(tmResource, source, target);
        log("Segments      : " + tuCount);

        OutputStream outputStream = new FileOutputStream(outputFile);

        XMLStreamWriter out = XMLOutputFactory.newInstance()
                .createXMLStreamWriter(new OutputStreamWriter(outputStream, "utf-8"));

        ObjectMapper mapper = new ObjectMapper();

        out.writeStartDocument();
        out.writeStartElement("tmx");
        out.writeAttribute("version", "1.4");

        out.writeStartElement("header");
        out.writeAttribute("creationtool", "SDLApiClient");
        out.writeAttribute("creationtoolversion", "SDLApiClient");
        out.writeAttribute("datatype", "xml");
        out.writeAttribute("segtype", "sentence");
        out.writeAttribute("adminLang", source);
        out.writeAttribute("srcLang", source);
        // out.writeAttribute("creationdate", "");
        // out.writeAttribute("creationid", "");
        out.writeEndElement();

        out.writeStartElement("body");

        TranslationUnits tus = app.getTus(tmResource.Id, source, target, 0, tuCount);
        if (tus == null) {
            tus = new TranslationUnits();
            tus.files = Collections.emptyList();
        }
        for (File tuFile : tus.files) {
            for (ParagraphUnit para : tuFile.paragraphUnits) {
                int sourceIdx = 0;
                for (Child sourcesc1 : para.source.children) {
                    int sourceTuIdx = 0;
                    for (Child sourceTu : sourcesc1.children) {

                        Child child = para.target.children.get(sourceIdx);
                        String jsonFields = child.metadata.systemFields;
                        SystemFields sysFields = mapper.readValue(jsonFields, SystemFields.class);

                        if (sourceTu.text == null || sourceTu.text.isEmpty()) {
                            // log("Empty source");
                            continue;
                        } else if (child.children.size() <= sourceTuIdx) {
                            // log("Skipping " + sourceTu.text);
                            continue;
                        }
                        Child targetTu = child.children.get(sourceTuIdx);

                        if (targetTu.text == null || targetTu.text.isEmpty()) {
                            // log("Empty target");
                            continue;
                        }

                        // log(jsonFields);

                        out.writeStartElement("tu");
                        out.writeAttribute("creationdate", sysFields.CreationDate.replaceAll("[-:]", ""));
                        out.writeAttribute("creationid", sysFields.CreationUser);
                        out.writeAttribute("changedate", sysFields.ChangeDate.replaceAll("[-:]", ""));
                        out.writeAttribute("changeid", sysFields.ChangeUser);
                        out.writeAttribute("lastusagedate", sysFields.UseDate.replaceAll("[-:]", ""));

                        out.writeStartElement("tuv");
                        out.writeAttribute("xml:lang", source);
                        out.writeStartElement("seg");
                        out.writeCharacters(sourceTu.text);
                        out.writeEndElement();
                        out.writeEndElement();
                        out.writeStartElement("tuv");
                        out.writeAttribute("xml:lang", target);
                        out.writeStartElement("seg");
                        out.writeCharacters(targetTu.text);
                        out.writeEndElement();
                        out.writeEndElement();
                        // log(sourceTu.text + "\t=>\t" + targetTu.text);
                        out.writeEndElement();

                        sourceTuIdx++;
                    }
                    sourceIdx++;
                }
            }
        }

        out.writeEndElement(); // </body>
        out.writeEndDocument();

        out.close();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        // <LanguageDirection Guid="85be37a4-3b7d-4e6e-b2a6-27ee5f4af0a2"
        // SettingsBundleGuid="9599ea6e-ee31-4337-bcad-c653f0e1e21d"
        // TargetLanguageCode="fr-CA" SourceLanguageCode="en-US">

        if (qName.equals("LanguageDirection")) {
            sourceLanguage = attributes.getValue("SourceLanguageCode");
            targetLanguage = attributes.getValue("TargetLanguageCode");
        } else if (qName.equals("MainTranslationProviderItem")) {
            providerUri.add(attributes.getValue("Uri"));
        }

        super.startElement(uri, localName, qName, attributes);
    }

    void setLogArea(JTextArea logArea) {
        this.logArea = logArea;
    }

    private void log(String s) {
        if (logArea != null) {
            logArea.append(s);
            logArea.append("\n");
        } else {
            System.out.println(s);
        }
    }

}
