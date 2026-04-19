package com.paperassistant.service.analysis;

/**
 * Prompt 模板常量类，定义各分析场景的 prompt 模板
 */
public final class PromptTemplates {

    private PromptTemplates() {
    }

    /**
     * 阶段 1: 逐篇深度理解
     */
    public static final String STAGE1_UNDERSTANDING = """
            你是一位资深学术研究顾问。请深度分析以下论文，提取关键信息：

            Title: {title}
            Abstract: {abstract}
            Citations: {citationCount}

            请以 JSON 格式返回：
            {
              "coreQuestion": "该论文要解决的核心问题 (1句话)",
              "methodologyCategory": "theoretical | experimental | survey | comparative",
              "keyContributions": ["贡献1", "贡献2", "贡献3"],
              "limitations": ["局限1", "局限2", "局限3"],
              "keywords": ["关键词1", ...],
              "methodsAndDatasets": ["使用的具体方法/模型/数据集"]
            }
            """;

    /**
     * 阶段 2: 交叉对比分析
     */
    public static final String STAGE2_CROSS_ANALYSIS = """
            以下是 {count} 篇论文的深度分析结果。请进行交叉对比：

            [论文分析列表...]

            请以 JSON 格式返回：
            {
              "topicClusters": [{"name": "簇名", "paperIndices": [0,1,2]}],
              "methodComparison": [{"method": "方法名", "papers": [0,3]}],
              "conflicts": [{"description": "矛盾点", "paperA": 0, "paperB": 2}],
              "consensusPoints": ["共识1", "共识2"],
              "methodologyGaps": ["没人用的方法"],
              "datasetGaps": ["没人覆盖的数据集"]
            }
            """;

    /**
     * 阶段 3: 创新机会生成
     */
    public static final String STAGE3_INNOVATION = """
            基于以下交叉分析结果，请生成研究空白、base 论文推荐和创新点：

            [交叉分析结果...]
            [原始论文信息...]

            注意：
            - 每个研究空白必须基于交叉分析的具体证据，不得凭空编造
            - base 论文优先选择引用数高、方法通用性强的论文
            - 创新点必须具体可执行，不得泛泛而谈
            - 每个创新点必须关联到交叉分析中的具体缺口

            请以 JSON 格式返回（注意：paperId 使用下方论文信息中的序号，从 0 开始）：
            {
              "gaps": [{"category": "分类", "description": "描述", "evidence": "证据", "supportingPaperIds": [0, 1, 2]}],
              "basePapers": [{"paperId": 0, "reason": "推荐理由", "innovationDirection": "创新方向"}],
              "innovationPts": [{"description": "创新点", "difficulty": "low|medium|high", "contributionType": "类型", "basePaperId": 0, "supportingGap": "关联的缺口"}]
            }
            """;
}
