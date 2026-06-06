package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PartDto(
    val text: String? = null,
    val inlineData: InlineDataDto? = null
)

@JsonClass(generateAdapter = true)
data class InlineDataDto(
    val mimeType: String,
    val data: String // base64 encoded payload
)

@JsonClass(generateAdapter = true)
data class ContentDto(
    val parts: List<PartDto>
)

@JsonClass(generateAdapter = true)
data class ImageConfigDto(
    val aspectRatio: String = "1:1",
    val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GenerationConfigDto(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseModalities: List<String>? = null, // e.g. ["TEXT", "IMAGE"]
    val imageConfig: ImageConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequestDto(
    val contents: List<ContentDto>,
    val generationConfig: GenerationConfigDto? = null,
    val systemInstruction: ContentDto? = null
)

@JsonClass(generateAdapter = true)
data class PartResponseDto(
    val text: String? = null,
    val inlineData: InlineDataDto? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponseDto(
    val parts: List<PartResponseDto>? = null
)

@JsonClass(generateAdapter = true)
data class CandidateDto(
    val content: ContentResponseDto? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponseDto(
    val candidates: List<CandidateDto>? = null
)
