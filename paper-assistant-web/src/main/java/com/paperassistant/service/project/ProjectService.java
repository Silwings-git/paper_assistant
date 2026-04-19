package com.paperassistant.service.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paperassistant.dto.CreateProjectRequest;
import com.paperassistant.dto.ProjectDTO;
import com.paperassistant.dto.UpdateProjectRequest;
import com.paperassistant.entity.Project;
import com.paperassistant.entity.ProjectStatus;
import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import com.paperassistant.mapper.ProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目管理服务
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private static final Set<String> VALID_STATUSES = Set.of(
            "CREATED", "SEARCHING", "SEARCHED", "ANALYZING", "ANALYZED"
    );

    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    /**
     * 创建项目
     */
    public ProjectDTO create(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setTopic(request.getTopic());
        project.setStatus(ProjectStatus.CREATED.name());

        projectMapper.insert(project);
        log.info("Project created: id={}, name={}", project.getId(), project.getName());

        return toDTO(project, 0);
    }

    /**
     * 更新项目
     */
    public ProjectDTO update(String id, UpdateProjectRequest request) {
        Project project = getEntityById(id);

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getTopic() != null) {
            project.setTopic(request.getTopic());
        }

        projectMapper.updateById(project);
        log.info("Project updated: id={}", id);

        int paperCount = projectMapper.countPapersByProjectId(id);
        return toDTO(project, paperCount);
    }

    /**
     * 根据 ID 获取项目
     */
    public ProjectDTO getById(String id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在: " + id);
        }
        int paperCount = projectMapper.countPapersByProjectId(id);
        return toDTO(project, paperCount);
    }

    /**
     * 获取项目实体 (内部使用)
     */
    public Project getEntityById(String id) {
        Project project = projectMapper.selectById(id);
        if (project == null || project.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在: " + id);
        }
        return project;
    }

    /**
     * 列表查询，包含论文数量
     */
    public List<ProjectDTO> list() {
        List<Map<String, Object>> rows = projectMapper.listWithPaperCount();
        return rows.stream()
                .map(row -> {
                    ProjectDTO dto = new ProjectDTO();
                    dto.setId(row.get("id").toString());
                    dto.setName((String) row.get("name"));
                    dto.setDescription((String) row.get("description"));
                    dto.setTopic((String) row.get("topic"));
                    dto.setStatus((String) row.get("status"));
                    dto.setBasePaperId(row.get("base_paper_id") != null ? row.get("base_paper_id").toString() : null);
                    dto.setPaperCount(((Number) row.get("paper_count")).intValue());
                    dto.setCreateTime(toLocalDateTime(row.get("create_time")));
                    dto.setUpdateTime(toLocalDateTime(row.get("update_time")));
                    return dto;
                })
                .toList();
    }

    /**
     * 逻辑删除项目，级联标记关联的 paper 和 analysis_result
     */
    @Transactional
    public void delete(String id) {
        Project project = getEntityById(id);
        projectMapper.softDeleteById(project.getId());

        // 级联标记关联的 paper
        cascadeSoftDelete("paper", id);
        // 级联标记关联的 analysis_result
        cascadeSoftDelete("analysis_result", id);

        log.info("Project deleted: id={}", id);
    }

    private void cascadeSoftDelete(String table, String projectId) {
        projectMapper.cascadeSoftDelete(table, projectId);
    }

    /**
     * 更新 base_paper_id
     */
    public ProjectDTO setBasePaper(String projectId, String paperId) {
        Project project = getEntityById(projectId);
        project.setBasePaperId(paperId);
        projectMapper.updateById(project);
        log.info("Base paper set: projectId={}, paperId={}", projectId, paperId);

        int paperCount = projectMapper.countPapersByProjectId(projectId);
        return toDTO(project, paperCount);
    }

    /**
     * 状态转换 (带校验)
     */
    @Transactional
    public void transitionStatus(String projectId, ProjectStatus newStatus) {
        Project project = getEntityById(projectId);
        ProjectStatus currentStatus = ProjectStatus.fromString(project.getStatus());

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new BusinessException(ErrorCode.STATE_TRANSITION_INVALID,
                    "非法状态转换: " + currentStatus.name() + " -> " + newStatus.name());
        }

        project.setStatus(newStatus.name());
        projectMapper.updateById(project);
        log.info("Project status transitioned: id={}, {} -> {}", projectId, currentStatus.name(), newStatus.name());
    }

    /**
     * 获取项目实体 (不抛异常版本，用于内部检查)
     */
    public Project getEntityByIdNoFail(String id) {
        return projectMapper.selectById(id);
    }

    /**
     * 校验状态转换是否合法
     */
    private boolean isValidTransition(ProjectStatus from, ProjectStatus to) {
        return switch (from) {
            case CREATED -> to == ProjectStatus.SEARCHING;
            case SEARCHING -> to == ProjectStatus.SEARCHED || to == ProjectStatus.CREATED;
            case SEARCHED -> to == ProjectStatus.ANALYZING || to == ProjectStatus.SEARCHING;
            case ANALYZING -> to == ProjectStatus.ANALYZED || to == ProjectStatus.SEARCHED;
            case ANALYZED -> to == ProjectStatus.SEARCHING;
        };
    }

    private ProjectDTO toDTO(Project project, int paperCount) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setTopic(project.getTopic());
        dto.setStatus(project.getStatus());
        dto.setBasePaperId(project.getBasePaperId());
        dto.setPaperCount(paperCount);
        dto.setCreateTime(project.getCreateTime());
        dto.setUpdateTime(project.getUpdateTime());
        return dto;
    }

    private java.time.LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt;
        }
        throw new IllegalArgumentException("Cannot convert to LocalDateTime: " + value.getClass());
    }
}
