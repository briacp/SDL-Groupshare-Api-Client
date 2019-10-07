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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import net.briac.sdl.model.Organization;
import net.briac.sdl.model.OrganizationResource;
import net.briac.sdl.model.SearchResults;
import net.briac.sdl.model.SearchText;
import net.briac.sdl.model.TranslationUnits;

public class SDLApiClient {

    private String restUri;
    private Client client;
    private String bearerToken;

    public SDLApiClient(String sdlServer, String username, String password) {
        client = ClientBuilder.newClient();
        restUri = sdlServer;
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder().credentials(username, password)
                .build();

        client
                // .register(new LoggingFeature(Logger.getLogger(getClass().getName()),
                // Level.INFO, null, null))
                .register(feature);
    }

    // http://gs2017dev.sdl.com:41234/documentation/api/index#!/Login/Login_signin
    public void login() {
        Response res = client.target(restUri).path("/authentication/api/1.0/login").request(MediaType.APPLICATION_JSON)
                .post(Entity.json(
                        "[\"ManagementRestApi\",\"ProjectServerRestApi\",\"MultiTermRestApi\",\"TMServerRestApi\"]"));

        bearerToken = res.readEntity(String.class).replaceAll("\"", ""); // THIS IS VERY UGLY

        // System.err.println("Bearer " + bearerToken);
    }

    public Map<String, Organization> getOrganizations() {
        Response res = client.target(restUri).path("/api/management/V2/organizations")
                .request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).get();

        Map<String, Organization> orgs = new HashMap<>();
        for (Organization org : res.readEntity(Organization[].class)) {
            orgs.put(org.Name, org);
        }

        return orgs;
    }

    public Map<String, OrganizationResource> getOrganizationResources(Organization org) {
        // api/management/V2/organizationresources/3fad5b31-a61f-41ee-a5c9-c836e6beacc9
        Response res = client.target(restUri)
                .path(UriBuilder.fromPath("api/management/V2/organizationresources/{organizationId}")
                        .build(org.UniqueId).getPath())
                .request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).get();
        Map<String, OrganizationResource> resources = new HashMap<>();
        for (OrganizationResource resource : res.readEntity(OrganizationResource[].class)) {
            resources.put(resource.Name, resource);
        }

        return resources;
    }

    public int getTuCount(OrganizationResource orgRes, String source, String target) {
        Response res = client.target(restUri)
                .path(UriBuilder.fromPath("api/tmservice/tms/{tmId}/tus/count").build(orgRes.Id).getPath())
                .queryParam("source", source).queryParam("target", target).request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).get();

        Integer tuCount = res.readEntity(Integer.class);

        if (tuCount == null) {
            System.err
                    .println("Cannot get TU count for resource \"" + orgRes.Id + "\" (" + source + "/" + target + ").");
            tuCount = 0;
        }
        return tuCount;
    }

    public TranslationUnits getTus(String orgResId, String source, String target, int startTuId, int count) {
        Response res = client.target(restUri)
                .path(UriBuilder.fromPath("api/tmservice/tms/{tmId}/tus").build(orgResId).getPath())
                // .register(new LoggingFeature(Logger.getLogger(getClass().getName()),
                // Level.INFO, null, null))
                .queryParam("source", source).queryParam("target", target).queryParam("startTuId", startTuId)
                .queryParam("count", count).request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).get();

        return res.readEntity(TranslationUnits.class);
    }

    public SearchResults searchConcordance(String tmId, String source, String target, SearchText searchText) {
        Response res = client.target(restUri)
                .path(UriBuilder.fromPath("api/tmservice/tms/{tmId}/search/concordance").build(tmId).getPath())
                // .register(new LoggingFeature(Logger.getLogger(getClass().getName()),
                // Level.INFO, null, null))
                .queryParam("source", source).queryParam("target", target).request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).post(Entity.json(searchText));

        // System.out.println(res.readEntity(String.class)); return null;
        return res.readEntity(SearchResults.class);
    }

}
