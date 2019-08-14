package com.jbequinn.syncserver.domain.model;


import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ChangesDto {
	private List<JsonObject> newItemsToSave = new ArrayList<>();
	private List<JsonObject> itemsToUpdate = new ArrayList<>();

	private List<JsonObject> newTagsToSave = new ArrayList<>();
	private List<JsonObject> tagsToUpdate = new ArrayList<>();

	private List<JsonObject> newDeletions = new ArrayList<>();
	private List<String> itemsIdsToDelete = new ArrayList<>();
	private List<String> tagIdsToDelete = new ArrayList<>();

	public List<JsonObject> getNewItemsToSave() {
		return newItemsToSave;
	}

	public void setNewItemsToSave(List<JsonObject> newItemsToSave) {
		this.newItemsToSave = newItemsToSave;
	}

	public List<JsonObject> getItemsToUpdate() {
		return itemsToUpdate;
	}

	public void setItemsToUpdate(List<JsonObject> itemsToUpdate) {
		this.itemsToUpdate = itemsToUpdate;
	}

	public List<JsonObject> getNewTagsToSave() {
		return newTagsToSave;
	}

	public void setNewTagsToSave(List<JsonObject> newTagsToSave) {
		this.newTagsToSave = newTagsToSave;
	}

	public List<JsonObject> getTagsToUpdate() {
		return tagsToUpdate;
	}

	public void setTagsToUpdate(List<JsonObject> tagsToUpdate) {
		this.tagsToUpdate = tagsToUpdate;
	}

	public List<JsonObject> getNewDeletions() {
		return newDeletions;
	}

	public void setNewDeletions(List<JsonObject> newDeletions) {
		this.newDeletions = newDeletions;
	}

	public List<String> getItemsIdsToDelete() {
		return itemsIdsToDelete;
	}

	public void setItemsIdsToDelete(List<String> itemsIdsToDelete) {
		this.itemsIdsToDelete = itemsIdsToDelete;
	}

	public List<String> getTagIdsToDelete() {
		return tagIdsToDelete;
	}

	public void setTagIdsToDelete(List<String> tagIdsToDelete) {
		this.tagIdsToDelete = tagIdsToDelete;
	}
}
