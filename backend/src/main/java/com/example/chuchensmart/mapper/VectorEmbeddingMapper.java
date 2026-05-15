package com.example.chuchensmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chuchensmart.entity.VectorEmbedding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 向量嵌入 Mapper
 * @author 小李
 */
@Mapper
public interface VectorEmbeddingMapper extends BaseMapper<VectorEmbedding> {
}
