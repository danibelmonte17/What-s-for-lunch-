package com.whatsforlunch.app.data.ai

import com.whatsforlunch.app.domain.scanner.AnalyzedFood

// Result of analyzing a scan photo with the AI: distinguishes success from a missing
// key (D5) and from any network/provider/parse failure (RD05).
sealed interface ScanAnalysisResult {
    data class Success(val foods: List<AnalyzedFood>) : ScanAnalysisResult
    data object MissingApiKey : ScanAnalysisResult
    data object Failed : ScanAnalysisResult
}

// Port of the AI food-image analysis. The image is only passed in memory (RD06).
interface ScanImageAnalyzer {
    suspend fun analyze(image: ByteArray, language: String): ScanAnalysisResult
}
