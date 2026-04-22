package com.oceanbase.importdata.controller;

import com.oceanbase.importdata.entity.DataSourceConfig;
import com.oceanbase.importdata.service.DataSourceManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasources")
@CrossOrigin(origins = "*")
public class DataSourceConfigController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfigController.class);

    private final DataSourceManagerService dataSourceManagerService;

    public DataSourceConfigController(DataSourceManagerService dataSourceManagerService) {
        this.dataSourceManagerService = dataSourceManagerService;
    }

    @GetMapping
    public ResponseEntity<List<DataSourceConfig>> getAllConfigs() {
        return ResponseEntity.ok(dataSourceManagerService.getAllConfigs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataSourceConfig> getConfig(@PathVariable String id) {
        DataSourceConfig config = dataSourceManagerService.getConfig(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @PostMapping
    public ResponseEntity<DataSourceConfig> createConfig(@RequestBody DataSourceConfig config) {
        try {
            DataSourceConfig created = dataSourceManagerService.createConfig(config);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Failed to create data source config", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataSourceConfig> updateConfig(@PathVariable String id,
                                                          @RequestBody DataSourceConfig config) {
        config.setConfigId(id);
        try {
            DataSourceConfig updated = dataSourceManagerService.updateConfig(config);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to update data source config", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        dataSourceManagerService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(@PathVariable String id) {
        dataSourceManagerService.setDefaultConfig(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            dataSourceManagerService.testConnection(id);
            result.put("success", true);
            result.put("message", "Connection test successful");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
