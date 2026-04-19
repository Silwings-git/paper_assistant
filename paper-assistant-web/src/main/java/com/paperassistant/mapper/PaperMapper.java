package com.paperassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paperassistant.entity.Paper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaperMapper extends BaseMapper<Paper> {

    /**
     * 幂等插入论文 (ON CONFLICT 跳过)
     */
    @Insert("""
            INSERT INTO paper (id, project_id, arxiv_id, source, source_id, title, abstract, authors,
                               publish_date, citation_count, influence_score, has_code,
                               code_url, pdf_url, category, is_deleted, create_time, update_time)
            VALUES (#{id}, #{projectId}, #{arxivId}, #{source}, #{sourceId}, #{title}, #{abstractText}, #{authors}::jsonb,
                    #{publishDate}, #{citationCount}, #{influenceScore}, #{hasCode},
                    #{codeUrl}, #{pdfUrl}, #{category}, 0, NOW(), NOW())
            ON CONFLICT (project_id, source, source_id) DO NOTHING
            """)
    int insertIgnore(@Param("id") String id,
                     @Param("projectId") String projectId,
                     @Param("arxivId") String arxivId,
                     @Param("source") String source,
                     @Param("sourceId") String sourceId,
                     @Param("title") String title,
                     @Param("abstractText") String abstractText,
                     @Param("authors") String authors,
                     @Param("publishDate") java.time.LocalDate publishDate,
                     @Param("citationCount") Integer citationCount,
                     @Param("influenceScore") Double influenceScore,
                     @Param("hasCode") Boolean hasCode,
                     @Param("codeUrl") String codeUrl,
                     @Param("pdfUrl") String pdfUrl,
                     @Param("category") String category);
}
