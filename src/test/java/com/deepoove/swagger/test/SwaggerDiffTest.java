package com.deepoove.swagger.test;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.Endpoint;
import com.deepoove.swagger.diff.output.HtmlRender;
import com.deepoove.swagger.diff.output.JsonRender;
import com.deepoove.swagger.diff.output.MarkdownRender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.swagger.models.HttpMethod;

public class SwaggerDiffTest {

	final String SWAGGER_V2_DOC1 = "petstore_v2_1.json";
	final String SWAGGER_V2_DOC2 = "petstore_v2_2.json";
	final String SWAGGER_V2_EMPTY_DOC = "petstore_v2_empty.json";
	final String SWAGGER_V2_HTTP = "http://petstore.swagger.io/v2/swagger.json";
	final String SWAGGER_V2_RESPONSE_NEW_MANDATORY_DOC1 = "response-changed/petstore_v2_1.yaml";
    final String SWAGGER_V2_RESPONSE_NEW_MANDATORY_DOC2 = "response-changed/petstore_v2_2.yaml";
    final String SWAGGER_V2_NESTED_NEW_MANDATORY_DOC1 = "nested-object-change-detail-new-mandatory/petstore_v2_1.yaml";
    final String SWAGGER_V2_NESTED_NEW_MANDATORY_DOC2 = "nested-object-change-detail-new-mandatory/petstore_v2_2.yaml";
    final String SWAGGER_V2_NESTED_FROM_MANDATORY_DOC1 = "nested-object-change-detail-mandatory-to-optional/petstore_v2_1.yaml";
    final String SWAGGER_V2_NESTED_FROM_MANDATORY_DOC2 = "nested-object-change-detail-mandatory-to-optional/petstore_v2_2.yaml";
    final String SWAGGER_V2_CHANGED_TYPE1 = "rename-type/petstore_v2_1.yaml";
    final String SWAGGER_V2_CHANGED_TYPE2 = "rename-type/petstore_v2_2.yaml";


    @Test
    public void testChangeType(){
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_CHANGED_TYPE1, SWAGGER_V2_CHANGED_TYPE2);
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        Assert.assertFalse(changedEndPoints.isEmpty());
    }


    @Test
    public void testNewMandatoryFieldInNestedObjectFomMandatoryToOptional(){
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_NESTED_FROM_MANDATORY_DOC1 , SWAGGER_V2_NESTED_FROM_MANDATORY_DOC2);
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        Assert.assertFalse(changedEndPoints.isEmpty());
    }

    @Test
    public void testNewMandatoryFieldInNestedObjectNewMandatory(){
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_NESTED_NEW_MANDATORY_DOC1 , SWAGGER_V2_NESTED_NEW_MANDATORY_DOC2);
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        Assert.assertFalse(changedEndPoints.isEmpty());
    }

	@Test
    public void testResponseNewMandatoryProperty(){
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_RESPONSE_NEW_MANDATORY_DOC1, SWAGGER_V2_RESPONSE_NEW_MANDATORY_DOC2);
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        Assert.assertFalse(changedEndPoints.isEmpty());
    }

	@Test
	public void testEqual() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC2, SWAGGER_V2_DOC2, true);
		assertEqual(diff);
	}

	@Test
	public void testNewApi() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_EMPTY_DOC, SWAGGER_V2_DOC2, true);
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		String html = new HtmlRender("Changelog",
				"http://deepoove.com/swagger-diff/stylesheets/demo.css")
						.render(diff);

		try {
			FileWriter fw = new FileWriter(
					"testNewApi.html");
			fw.write(html);
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue(newEndpoints.size() > 0);
		Assert.assertTrue(missingEndpoints.isEmpty());
		Assert.assertTrue(changedEndPoints.isEmpty());

	}

	@Test
	public void testDeprecatedApi() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_EMPTY_DOC, true);
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		String html = new HtmlRender("Changelog",
				"http://deepoove.com/swagger-diff/stylesheets/demo.css")
						.render(diff);

		try {
			FileWriter fw = new FileWriter(
					"testDeprecatedApi.html");
			fw.write(html);
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue(newEndpoints.isEmpty());
		Assert.assertTrue(missingEndpoints.size() > 0);
		Assert.assertTrue(changedEndPoints.isEmpty());

	}

	private void assertVendorExtensionsAreDiff(ChangedExtensionGroup vendorExtensions) {
		Assert.assertTrue(vendorExtensions.vendorExtensionsAreDiff());
	}
	
	@Test
	public void testDiff() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2, true);
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		String html = new HtmlRender("Changelog",
				"src/main/resources/demo.css")
				.render(diff);
		
		try {
			FileWriter fw = new FileWriter(
					"testDiff.html");
			fw.write(html);
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		ChangedExtensionGroup tlVendorExts = diff.getChangedVendorExtensions();
		assertVendorExtensionsAreDiff(tlVendorExts);
		for (String key : tlVendorExts.getChangedSubGroups().keySet()) {
			assertVendorExtensionsAreDiff(tlVendorExts.getChangedSubGroups().get(key));
		}

		List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
		for (ChangedEndpoint changedEndpoint : changedEndpoints) {
			if (changedEndpoint.getPathUrl().equals("/pet")) {
				assertVendorExtensionsAreDiff(changedEndpoint);

				ChangedOperation changedOperation = changedEndpoint.getChangedOperations().get(HttpMethod.POST);
				assertVendorExtensionsAreDiff(changedOperation);
			}
		}
//		Assert.assertFalse(changedEndPoints.isEmpty());
	}
	
	@Test
	public void testDiffAndMarkdown() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2, true);
		String render = new MarkdownRender().render(diff);
		try {
			FileWriter fw = new FileWriter(
					"testDiff.md");
			fw.write(render);
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testEqualJson() {
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SWAGGER_V2_DOC1);
			JsonNode json = new ObjectMapper().readTree(inputStream);
			SwaggerDiff diff = SwaggerDiff.compareV2(json, json, true);
			assertEqual(diff);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	@Test
	public void testJsonRender() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		String render = new JsonRender().render(diff);
		try {
			FileWriter fw = new FileWriter(
					"testDiff.json");
			fw.write(render);
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void assertEqual(SwaggerDiff diff) {
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		Assert.assertTrue(newEndpoints.isEmpty());
		Assert.assertTrue(missingEndpoints.isEmpty());
		Assert.assertTrue(changedEndPoints.isEmpty());

	}

}
