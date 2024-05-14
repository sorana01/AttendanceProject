package com.example.attendanceproject.account.adapters;

public class EntityItem {
    private String entityName;
    private String entityDetail;
    private String entityId;

    public EntityItem(String entityName, String entityDetail) {
        this.entityName = entityName;
        this.entityDetail = entityDetail;
    }

    public EntityItem(String entityName) {
        this.entityName = entityName;
    }

    public EntityItem(String entityName, String entityDetail, String entityId) {
        this.entityName = entityName;
        this.entityDetail = entityDetail;
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityDetail() {
        return entityDetail;
    }

    public void setEntityDetail(String entityDetail) {
        this.entityDetail = entityDetail;
    }
}
