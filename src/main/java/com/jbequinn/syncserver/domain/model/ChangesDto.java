package com.jbequinn.syncserver.domain.model;

import lombok.Data;

import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChangesDto {
	private List<JsonObject> newItemsToSave = new ArrayList<>();
	private List<JsonObject> itemsToUpdate = new ArrayList<>();

	private List<JsonObject> newTagsToSave = new ArrayList<>();
	private List<JsonObject> tagsToUpdate = new ArrayList<>();

	private List<JsonObject> newDeletions = new ArrayList<>();
	private List<String> itemsIdsToDelete = new ArrayList<>();
	private List<String> tagIdsToDelete = new ArrayList<>();
}
