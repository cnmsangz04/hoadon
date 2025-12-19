package vn.hoadon.dto.permission;

import lombok.Data;

@Data
public class PermissionCreateDTO {
    private Long id;
    private Long category;
    private String name;
    private Integer level;
    private String displayName;
    private String description;

    // Explicit getters/setters to ensure availability even if Lombok is not processed
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategory() { return category; }
    public void setCategory(Long category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}