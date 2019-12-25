package com.deepoove.swagger.diff.compare;

import java.util.*;

import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.ChangedExtensionGroup;
import com.deepoove.swagger.diff.model.ChangedOperation;
import com.deepoove.swagger.diff.model.ChangedParameter;
import com.deepoove.swagger.diff.model.Endpoint;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * compare two Swagger
 *
 * @author Sayi
 *
 */
public class SpecificationDiff extends ChangedExtensionGroup {

	private List<Endpoint> newEndpoints;
	private List<Endpoint> missingEndpoints;
	private List<ChangedEndpoint> changedEndpoints;

	private SpecificationDiff() {
	}

	public static SpecificationDiff diff(Swagger oldSpec, Swagger newSpec) {
		return diff(oldSpec, newSpec, true);
	}

	public static SpecificationDiff diff(Swagger oldSpec, Swagger newSpec, boolean withExtensions) {
		if (null == oldSpec || null == newSpec) { throw new IllegalArgumentException(
				"cannot diff null spec."); }
		SpecificationDiff instance = new SpecificationDiff();
		VendorExtensionDiff extDiffer = new VendorExtensionDiff(withExtensions);
		if (null == oldSpec || null == newSpec) {
			throw new IllegalArgumentException("cannot diff null spec.");
		}
		Map<String, Path> oldPaths = oldSpec.getPaths();
		Map<String, Path> newPaths = newSpec.getPaths();

		// Diff path
		MapKeyDiff<String, Path> pathDiff = MapKeyDiff.diff(oldPaths, newPaths);
		instance.newEndpoints = convert2EndpointList(pathDiff.getIncreased());
		instance.missingEndpoints = convert2EndpointList(pathDiff.getMissing());
		instance.changedEndpoints = new ArrayList<>();

		instance.setVendorExtsFromGroup(extDiffer.diff(oldSpec, newSpec));
		instance.putSubGroup("info", extDiffer.diff(oldSpec.getInfo(), newSpec.getInfo()));


		List<String> sharedKey = pathDiff.getSharedKey();
		sharedKey.stream().forEach((pathUrl) -> {
			ChangedEndpoint changedEndpoint = new ChangedEndpoint();
			changedEndpoint.setPathUrl(pathUrl);
			Path oldPath = oldPaths.get(pathUrl);
			Path newPath = newPaths.get(pathUrl);

			changedEndpoint.setVendorExtsFromGroup(extDiffer.diff(oldPath, newPath));

			// Diff Operation
			Map<HttpMethod, Operation> oldOperationMap = oldPath.getOperationMap();
			Map<HttpMethod, Operation> newOperationMap = newPath.getOperationMap();
			MapKeyDiff<HttpMethod, Operation> operationDiff = MapKeyDiff.diff(oldOperationMap,
					newOperationMap);
			Map<HttpMethod, Operation> increasedOperation = operationDiff.getIncreased();
			Map<HttpMethod, Operation> missingOperation = operationDiff.getMissing();
			changedEndpoint.setNewOperations(increasedOperation);
			changedEndpoint.setMissingOperations(missingOperation);

			List<HttpMethod> sharedMethods = operationDiff.getSharedKey();
			Map<HttpMethod, ChangedOperation> operas = new HashMap<>();
			sharedMethods.stream().forEach((method) -> {
				ChangedOperation changedOperation = new ChangedOperation();
				Operation oldOperation = oldOperationMap.get(method);
				Operation newOperation = newOperationMap.get(method);
				changedOperation.setSummary(newOperation.getSummary());

				changedOperation.setVendorExtsFromGroup(extDiffer.diff(oldOperation, newOperation));

				// Diff Parameter
				List<Parameter> oldParameters = oldOperation.getParameters();
				List<Parameter> newParameters = newOperation.getParameters();
				ParameterDiff parameterDiff = ParameterDiff
						.buildWithDefinition(oldSpec.getDefinitions(), newSpec.getDefinitions())
						.diff(oldParameters, newParameters);
				changedOperation.setAddParameters(parameterDiff.getIncreased());
				changedOperation.setMissingParameters(parameterDiff.getMissing());
				changedOperation.setChangedParameter(parameterDiff.getChanged());

				for (ChangedParameter param : parameterDiff.getChanged()) {
					param.setVendorExtsFromGroup(extDiffer.diff(param.getLeftParameter(), param.getRightParameter()));
				}

				// Diff response
				Property oldResponseProperty = getResponseProperty(oldOperation);
				Property newResponseProperty = getResponseProperty(newOperation);
				PropertyDiff propertyDiff = PropertyDiff
						.buildWithDefinition(oldSpec.getDefinitions(), newSpec.getDefinitions());
				propertyDiff.diff(oldResponseProperty, newResponseProperty);
				changedOperation.setAddProps(propertyDiff.getIncreased());
				changedOperation.setMissingProps(propertyDiff.getMissing());
				changedOperation.setChangedProps(propertyDiff.getChanged());

				changedOperation.putSubGroup("responses",
						extDiffer.diffResGroup(oldOperation.getResponses(), newOperation.getResponses()));

				if (changedOperation.isDiff()) {
					operas.put(method, changedOperation);
				}
			});
			changedEndpoint.setChangedOperations(operas);

			instance.newEndpoints.addAll(convert2EndpointList(changedEndpoint.getPathUrl(),
					changedEndpoint.getNewOperations()));
			instance.missingEndpoints.addAll(convert2EndpointList(changedEndpoint.getPathUrl(),
					changedEndpoint.getMissingOperations()));

			if (changedEndpoint.isDiff()) {
				instance.changedEndpoints.add(changedEndpoint);
			}
		});

		instance.putSubGroup("securityDefinitions",
				extDiffer.diffSecGroup(oldSpec.getSecurityDefinitions(), newSpec.getSecurityDefinitions()));

		instance.putSubGroup("tags",
				extDiffer.diffTagGroup(mapTagsByName(oldSpec.getTags()), mapTagsByName(newSpec.getTags())));

		return instance;

	}

	private static Map<String, Tag> mapTagsByName(List<Tag> tags) {
		Map<String, Tag> mappedTags = new LinkedHashMap();
		if (tags == null) {
			return mappedTags;
		}
		for (Tag tag : tags) {
			mappedTags.put(tag.getName(), tag);
		}
		return mappedTags;
	}

	private static Property getResponseProperty(Operation operation) {
		Map<String, Response> responses = operation.getResponses();
		// temporary workaround for missing response messages
		if (responses == null) return null;
		Response response = responses.get("200");
		return null == response ? null : response.getSchema();
	}

	private static List<Endpoint> convert2EndpointList(Map<String, Path> map) {
		List<Endpoint> endpoints = new ArrayList<Endpoint>();
		if (null == map) return endpoints;
		map.forEach((url, path) -> {
			Map<HttpMethod, Operation> operationMap = path.getOperationMap();
			operationMap.forEach((httpMethod, operation) -> {
				Endpoint endpoint = new Endpoint();
				endpoint.setPathUrl(url);
				endpoint.setMethod(httpMethod);
				endpoint.setSummary(operation.getSummary());
				endpoint.setPath(path);
				endpoint.setOperation(operation);
				endpoints.add(endpoint);
			});
		});
		return endpoints;
	}

	private static Collection<? extends Endpoint> convert2EndpointList(String pathUrl,
																	   Map<HttpMethod, Operation> map) {
		List<Endpoint> endpoints = new ArrayList<Endpoint>();
		if (null == map) return endpoints;
		map.forEach((httpMethod, operation) -> {
			Endpoint endpoint = new Endpoint();
			endpoint.setPathUrl(pathUrl);
			endpoint.setMethod(httpMethod);
			endpoint.setSummary(operation.getSummary());
			endpoint.setOperation(operation);
			endpoints.add(endpoint);
		});
		return endpoints;
	}

	public List<Endpoint> getNewEndpoints() {
		return newEndpoints;
	}

	public List<Endpoint> getMissingEndpoints() {
		return missingEndpoints;
	}

	public List<ChangedEndpoint> getChangedEndpoints() {
		return changedEndpoints;
	}

}