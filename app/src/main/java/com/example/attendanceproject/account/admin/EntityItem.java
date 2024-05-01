package com.example.attendanceproject.account.admin;

public class EntityItem {
    private String entityName;
    private String entityDetail;

    public EntityItem(String entityName, String entityDetail) {
        this.entityName = entityName;
        this.entityDetail = entityDetail;
    }

    public String getEntityName() {
        return entityName;
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
