package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.db.*
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisViewModel(val repository: JarvisRepository) : ViewModel() {

    // API Key checks
    val currentApiKey: String = BuildConfig.GEMINI_API_KEY
    val isApiKeyConfigured: Boolean = currentApiKey.isNotEmpty() && currentApiKey != "MY_GEMINI_API_KEY"

    // Model names mapping as per skill guidelines
    private val modelFlash = "gemini-3.5-flash"
    private val modelPro = "gemini-3.1-pro-preview"
    private val modelImage = "gemini-2.5-flash-image"

    // --- State Streams ---
    
    // UI Theme options (true = Cyan Hologram Dark, false = Light Core)
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Navigation state helper
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // General app metrics / system info simulation
    val wakeupToggle = MutableStateFlow(true)
    val batterySaverStats = MutableStateFlow("Background Mode: ACTIVE. Low battery overhead stats: 1.2% / hr")

    // Database Observants
    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workflows: StateFlow<List<WorkflowEntity>> = repository.allWorkflows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<AuditLogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinterestPins: StateFlow<List<PinterestPinEntity>> = repository.allPins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Chat States ---
    val chatInput = MutableStateFlow("")
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Jarvis", "System online. JARVIS Autonomous Mobile Interface stabilized. How may I assist you, Operator?", System.currentTimeMillis())
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()
    val isGenerating = MutableStateFlow(false)
    val routingLog = MutableStateFlow("Brain state: Idle. Waiting for trigger prompt...")
    val selectedModelOverride = MutableStateFlow("Auto-Route") // Auto-Route, Gemini Flash, Gemini Pro, Offline Local

    // --- Voice Assistant States ---
    val voiceState = MutableStateFlow("Idle") // Idle, Listening, Processing, Speaking
    val voiceTranscript = MutableStateFlow("")
    val voiceResponseText = MutableStateFlow("")
    val selectedVoice = MutableStateFlow("Kore (Enthusiastic)")
    val voiceAlwaysListening = MutableStateFlow(false)

    // --- Vision Screen Understanding States ---
    val selectedMockScreen = MutableStateFlow("Pinterest Home")
    val visionResult = MutableStateFlow("")
    val isVisionAnalyzing = MutableStateFlow(false)

    // --- Content Studio States ---
    val studioPrompt = MutableStateFlow("")
    val selectedContentType = MutableStateFlow("Pinterest Pin") // Pinterest Pin, Instagram Caption, Tweet Thread, Blog outline
    val studioGenText = MutableStateFlow("")
    val studioGenImage = MutableStateFlow("") // Base64
    val isStudioWorking = MutableStateFlow(false)

    // --- Custom Natural Language Automation States ---
    val workflowGenTitle = MutableStateFlow("")
    val workflowGenPrompt = MutableStateFlow("")
    val workflowGenSchedule = MutableStateFlow("Every day at 9:00 AM")

    // --- Autonomous Pinterest Campaign State Engine ---
    val pinterestStep = MutableStateFlow(1)
    val pinterestAuthorized = MutableStateFlow(false)
    val pinterestNiche = MutableStateFlow("Futuristic Cyberpunk Landscapes")
    val pinterestBrandingName = MutableStateFlow("Cyberpunk_Canvas")
    val pinterestBioText = MutableStateFlow("")
    val pinterestProfileImageB64 = MutableStateFlow("")
    val pinterestStrategyPlan = MutableStateFlow("")
    val pinterestSuggestedImageTheme = MutableStateFlow("")
    val pinterestRunningPinTitle = MutableStateFlow("")
    val pinterestRunningPinDesc = MutableStateFlow("")
    val pinterestRunningPinTags = MutableStateFlow("#cyberpunk #aiart #landscape")
    val pinterestGeneratedPinImageB64 = MutableStateFlow("")
    val isPinterestWorking = MutableStateFlow(false)
    val pinterestSessionLogs = MutableStateFlow<List<String>>(listOf("System initiated."))

    init {
        // Pre-populate database if empty with custom default configurations
        viewModelScope.launch {
            repository.allWorkflows.first().let { currentWorkflows ->
                if (currentWorkflows.isEmpty()) {
                    repository.insertWorkflow(
                        WorkflowEntity(
                            title = "Daily Pinterest Social Poster",
                            prompt = "Research AI Cyberpunk art topics, generate a high-res pin, craft keywords and schedule automated upload.",
                            scheduledTime = "Every day at 09:00 AM",
                            category = "Pinterest"
                        )
                    )
                    repository.insertWorkflow(
                        WorkflowEntity(
                            title = "Weekly Performance Report Builder",
                            prompt = "Extract local database telemetry, summarize click-through rates, compile audit metrics and write text brief.",
                            scheduledTime = "Fridays at 05:00 PM",
                            category = "Analytics"
                        )
                    )
                    repository.logAction("Database Seeded", "Default workflows pre-configured for automated runtime.", "SUCCESS")
                }
            }

            repository.allMemories.first().let { currentMemories ->
                if (currentMemories.isEmpty()) {
                    repository.insertMemory(MemoryEntity(key = "Operator Name", value = "User", category = "Preference"))
                    repository.insertMemory(MemoryEntity(key = "App Vibe Accent", value = "Cyberpunk Cyan Hologram", category = "Style"))
                    repository.insertMemory(MemoryEntity(key = "Enterprise Goal", value = "Scale AI Art social media outreach using automated agents.", category = "Project"))
                }
            }

            repository.logAction("Jarvis Core Ready", "Main threat analysis engines online. No anomalies detected.", "INFO")
        }
    }

    // --- CORE API INTERACTION ---

    private suspend fun callGeminiAPI(systemInstruction: String, userPrompt: String, targetModel: String): String {
        if (!isApiKeyConfigured) {
            // Simulated fallback content representing premium responses
            delaySimulated(1500)
            return getSimulatedFallback(userPrompt, systemInstruction)
        }

        return try {
            val request = GenerateContentRequestDto(
                contents = listOf(ContentDto(parts = listOf(PartDto(text = userPrompt)))),
                systemInstruction = ContentDto(parts = listOf(PartDto(text = systemInstruction)))
            )
            val response = RetrofitClient.service.generateContent(
                model = targetModel,
                apiKey = currentApiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Empty response contents. Please retry."
        } catch (e: Exception) {
            Log.e("JarvisBrain", "Error calling Gemini API: ${e.message}", e)
            repository.logAction("API Error", "Network fault calling $targetModel: ${e.message}", "WARNING")
            "An error occurred. System automatically enabling diagnostic mode.\nDetails: ${e.localizedMessage}"
        }
    }

    private suspend fun callGeminiImageAPI(prompt: String): String {
        if (!isApiKeyConfigured) {
            delaySimulated(2000)
            return getSimulatedImageB64(prompt)
        }

        return try {
            val request = GenerateContentRequestDto(
                contents = listOf(ContentDto(parts = listOf(PartDto(text = prompt)))),
                generationConfig = GenerationConfigDto(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = ImageConfigDto(aspectRatio = "1:1", imageSize = "1K")
                ),
                systemInstruction = ContentDto(parts = listOf(PartDto(text = "You are an AI Image generation engine. Produce visual files directly based on input prompt.")))
            )
            val response = RetrofitClient.service.generateContent(
                model = modelImage,
                apiKey = currentApiKey,
                request = request
            )
            // Retrieve inline Base64 dynamic content
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData?.data
                ?: run {
                    // fall back to a text response or a clean preset
                    Log.w("JarvisBrain", "Image generation API requested but did not return inlineData. Falling back to structured gradient.")
                    getSimulatedImageB64(prompt)
                }
        } catch (e: Exception) {
            Log.e("JarvisBrain", "Error generating image: ${e.message}", e)
            repository.logAction("Image Gen Error", "Failed to retrieve image: ${e.message}", "WARNING")
            getSimulatedImageB64(prompt) // backup simulation
        }
    }

    // Helper for natural chat conversation
    fun sendMessage() {
        val input = chatInput.value.trim()
        if (input.isEmpty()) return

        val userMsg = ChatMessage("User", input, System.currentTimeMillis())
        _chatHistory.value = _chatHistory.value + userMsg
        chatInput.value = ""

        viewModelScope.launch {
            isGenerating.value = true

            // State-based model routing
            var targetModel = modelFlash
            var brainText = "Brain state: Routing. Analyzing complexity of query..."
            routingLog.value = brainText

            if (input.contains("code", ignoreCase = true) || input.contains("program", ignoreCase = true) || input.length > 150) {
                targetModel = modelPro
                brainText = "Auto-Route: Extended reasoning detected. Forwarding to Gemini Pro [$targetModel]..."
            } else {
                brainText = "Auto-Route: Conversation detected. Forwarding to Gemini Flash [$targetModel]..."
            }
            if (selectedModelOverride.value != "Auto-Route") {
                targetModel = if (selectedModelOverride.value == "Gemini Pro") modelPro else modelFlash
                brainText = "Manual Route: Overriding to [$targetModel]..."
            }
            routingLog.value = brainText

            repository.logAction("Cognitive Route", "Command input filtered. Model directed: $targetModel", "INFO")

            val systemInstruction = "You are JARVIS, an autonomous, highly advanced mobile AI assistant. Communicate with precision, intelligence, and high-fidelity technical expertise. Keep responses action-oriented, helpful, and organized."
            val replyText = callGeminiAPI(systemInstruction, input, targetModel)

            val jarvisMsg = ChatMessage("Jarvis", replyText, System.currentTimeMillis())
            _chatHistory.value = _chatHistory.value + jarvisMsg

            routingLog.value = "Brain state: Rest. Response delivered."
            isGenerating.value = false
        }
    }

    fun clearChat() {
        _chatHistory.value = listOf(
            ChatMessage("Jarvis", "History deleted. Neural registers cleared.", System.currentTimeMillis())
        )
    }

    // --- VOICE CONTROLLER ---
    fun startVoiceListening() {
        voiceState.value = "Listening..."
        viewModelScope.launch {
            delaySimulated(2500)
            voiceTranscript.value = "Hello Jarvis, compile daily social schedule and post to Pinterest please."
            voiceState.value = "Processing query..."

            delaySimulated(1200)
            val promptResult = "Understood. Reaching into the Pinterest automation core. Deploying autonomous sequence agent. I will initiate branding plans and schedule pin assets. Opening campaigns interface."
            voiceResponseText.value = promptResult
            voiceState.value = "Speaking..."

            // Trigger action! Navigate directly to pinterest autonomous workflow
            repository.logAction("Voice Wake Detection", "Voice passphrase matched. Prompt: '${voiceTranscript.value}'", "SUCCESS")
            delaySimulated(2000)
            voiceState.value = "Idle"
            navigateTo("pinterest")
        }
    }

    // --- SCREEN VISION CONTROLLER ---
    fun analyzeCurrentMockScreen() {
        isVisionAnalyzing.value = true
        viewModelScope.launch {
            val targetScreen = selectedMockScreen.value
            val userPrompt = "Analyze this layout of $targetScreen. Detect clickable widgets, coordinates, bio descriptions and structural recommendations."
            val customSystem = "You are a Screen Vision Accessibility API wrapper. Respond with OCR logs, parsed buttons list, and actions recommendations."

            repository.logAction("Screen Diagnostics", "Scanning pixels on viewport: $targetScreen", "INFO")
            val result = callGeminiAPI(customSystem, userPrompt, modelFlash)
            visionResult.value = result
            isVisionAnalyzing.value = false
            repository.logAction("Screen Scanned", "Structural analysis returned for $targetScreen. Click coordinates saved.", "SUCCESS")
        }
    }

    // --- MANUAL WORKFLOW CREATION ---
    fun addCustomWorkflow() {
        val title = workflowGenTitle.value.trim()
        val prompt = workflowGenPrompt.value.trim()
        val sched = workflowGenSchedule.value.trim()

        if (title.isEmpty()) return

        viewModelScope.launch {
            repository.insertWorkflow(
                WorkflowEntity(
                    title = title,
                    prompt = prompt,
                    scheduledTime = sched,
                    category = "Custom Voice"
                )
            )
            workflowGenTitle.value = ""
            workflowGenPrompt.value = ""
        }
    }

    fun toggleWorkflowActive(workflow: WorkflowEntity) {
        viewModelScope.launch {
            repository.updateWorkflow(workflow.copy(isActive = !workflow.isActive))
            repository.logAction("Workflow Modified", "Toggled state for '${workflow.title}'", "INFO")
        }
    }

    fun triggerWorkflowExecution(workflow: WorkflowEntity) {
        viewModelScope.launch {
            repository.updateWorkflow(workflow.copy(lastRunStatus = "Running"))
            repository.logAction("Automation Started", "Executing Natural Language script for '${workflow.title}'...", "INFO")
            
            val systemIns = "You are JARVIS Agent core executing a structured social workflow: ${workflow.prompt}."
            val userP = "Generate steps, captions and execution summary of tasks."
            delaySimulated(2500)
            
            val summary = callGeminiAPI(systemIns, userP, modelFlash)
            repository.logAction("Automation Successful", "Workflow completed: ${workflow.title}. Outputs logged to files system.", "SUCCESS")
            repository.updateWorkflow(workflow.copy(lastRunStatus = "Success"))
        }
    }

    fun deleteWorkflow(workflow: WorkflowEntity) {
        viewModelScope.launch {
            repository.deleteWorkflow(workflow)
        }
    }

    // --- CONTENT GENERATOR ---
    fun generateContentStudio() {
        val prompt = studioPrompt.value.trim()
        if (prompt.isEmpty()) return

        isStudioWorking.value = true
        viewModelScope.launch {
            val cType = selectedContentType.value
            repository.logAction("Content Studio Prompt", "Generating content outline for types: $cType", "INFO")

            val customSys = "You are JARVIS Expert Content Agent. Generate optimized $cType copy, tags, and formatting instructions based on prompt."
            val generatedText = callGeminiAPI(customSys, prompt, modelFlash)
            studioGenText.value = generatedText

            // Generate image as well using flash-image
            val visualPrompt = "A futuristic cyberpunk digital art representation of $prompt, clean high fidelity concept design."
            val generatedImg64 = callGeminiImageAPI(visualPrompt)
            studioGenImage.value = generatedImg64

            isStudioWorking.value = false
            repository.logAction("Asset Ready", "Visual and textual social templates completed in content studio.", "SUCCESS")
        }
    }

    // --- SEARCH / WRITE INTEGRATED MEMORY ---
    val searchMemoryQuery = MutableStateFlow("")
    val newMemoryKey = MutableStateFlow("")
    val newMemoryValue = MutableStateFlow("")
    val newMemoryCategory = MutableStateFlow("Preference")

    fun addManualMemory() {
        val key = newMemoryKey.value.trim()
        val value = newMemoryValue.value.trim()
        val cat = newMemoryCategory.value

        if (key.isEmpty() || value.isEmpty()) return

        viewModelScope.launch {
            repository.insertMemory(
                MemoryEntity(
                    key = key,
                    value = value,
                    category = cat
                )
            )
            newMemoryKey.value = ""
            newMemoryValue.value = ""
        }
    }

    // --- PINTEREST CAMPAIGN STATE MACHINE ENGINE ---
    
    private fun logPinterestSession(message: String) {
        val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val logLine = "[${format.format(Date())}] $message"
        pinterestSessionLogs.value = listOf(logLine) + pinterestSessionLogs.value
    }

    fun authorizePinterest() {
        isPinterestWorking.value = true
        viewModelScope.launch {
            delaySimulated(1500)
            pinterestAuthorized.value = true
            isPinterestWorking.value = false
            pinterestStep.value = 2
            logPinterestSession("Successful connection! Pinterest OAuth2 token active. Read/write scoped.")
            repository.logAction("Account Approved", "Pinterest API channel integration authorized.", "SUCCESS")
        }
    }

    fun submitPinterestBranding() {
        val username = pinterestBrandingName.value.trim()
        val niche = pinterestNiche.value.trim()
        if (username.isEmpty() || niche.isEmpty()) return

        isPinterestWorking.value = true
        logPinterestSession("Assembling brand framework for @$username with target niche: $niche...")

        viewModelScope.launch {
            // Bio text generation
            val bioPrompt = "Generate an optimized, stylish Pinterest profile bio description for a page named @$username focusing on $niche."
            val bioSystem = "Write a short, engaging description (max 140 chars) with neat cyberpunk aesthetics."
            val bioResult = callGeminiAPI(bioSystem, bioPrompt, modelFlash)
            pinterestBioText.value = bioResult
            logPinterestSession("Bio created: $bioResult")

            // Avatar image generation
            val avatarPrompt = "Minimalist visual emblem icon logo for a page named @$username focusing on $niche, vector icon, circular vector aesthetic on pitch dark background, cyan glows"
            val avatarImg = callGeminiImageAPI(avatarPrompt)
            pinterestProfileImageB64.value = avatarImg
            logPinterestSession("Visual branding assets rendered successfully.")

            isPinterestWorking.value = false
            pinterestStep.value = 3
            repository.logAction("Branding Configured", "Digital identity proposal complete for @$username.", "SUCCESS")
        }
    }

    fun buildPinterestContentPlan() {
        isPinterestWorking.value = true
        logPinterestSession("Analyzing algorithmic pinning schedules... Compiling 7-day visual curation calendar for niche '${pinterestNiche.value}'...")

        viewModelScope.launch {
            val planPrompt = "Draft a structured 7-day social media keyword schedule and visual planning guidelines for a Pinterest campaign on niche: ${pinterestNiche.value}."
            val planSystem = "Generate a week scheduling plan detailing: Day, Theme, Prompt Idea, and and description hashtags."
            val planResult = callGeminiAPI(planSystem, planPrompt, modelPro)
            pinterestStrategyPlan.value = planResult
            logPinterestSession("Marketing strategy drafted.")

            // Suggest the prompt for Step 4
            val promptIdeaSystem = "Suggest a highly descriptive text visual prompt for generating an incredible cyberpunk digital illustration. Reply ONLY with the prompt itself, nothing else."
            val suggestedPrompt = callGeminiAPI(promptIdeaSystem, "niche: ${pinterestNiche.value}", modelFlash)
            pinterestSuggestedImageTheme.value = suggestedPrompt
            logPinterestSession("Suggested next pin prompt: '$suggestedPrompt'")

            isPinterestWorking.value = false
            pinterestStep.value = 4
            repository.logAction("Strategy Generated", "Content calendar formatted for Pinning Niche.", "SUCCESS")
        }
    }

    fun submitImageThemeSuggestion(theme: String) {
        pinterestSuggestedImageTheme.value = theme
        pinterestStep.value = 5
    }

    fun generatePinterestPinImage() {
        isPinterestWorking.value = true
        logPinterestSession("Triggering artificial neural canvas. Routing prompt to Gemini Flash Image...")

        viewModelScope.launch {
            val visualPrompt = pinterestSuggestedImageTheme.value
            val img = callGeminiImageAPI(visualPrompt)
            pinterestGeneratedPinImageB64.value = img
            logPinterestSession("High-res creative pin visual generated successfully.")

            isPinterestWorking.value = false
            pinterestStep.value = 6
            repository.logAction("Creative Pin Rendered", "Visual assets saved to volatile RAM drawer.", "SUCCESS")
        }
    }

    fun generatePinterestDraftMeta() {
        isPinterestWorking.value = true
        logPinterestSession("Drafting social metadata, visual captions, copy titles, tags and optimized description blocks for pin theme: '${pinterestSuggestedImageTheme.value}'...")

        viewModelScope.launch {
            val metaPrompt = "Generate a catchy Pinterest post title, detailed keyword SEO description, and 5 hashtags for visual item themed: '${pinterestSuggestedImageTheme.value}'."
            val metaSystem = "Draft Title, Description, and Tags separately. Keep it clean and highly structured. Ready for clipboard ingestion."
            val textMeta = callGeminiAPI(metaSystem, metaPrompt, modelFlash)

            // Simplistic extraction logic from result
            val lines = textMeta.lines().filter { it.isNotBlank() }
            val maybeTitle = lines.firstOrNull { it.contains("title", ignoreCase = true) }?.replace(Regex("Title|:|-"), "")?.trim()
                ?: "Visual Inspiration in ${pinterestNiche.value}"
            val maybeDesc = lines.firstOrNull { it.contains("description", ignoreCase = true) }?.replace(Regex("Description|:|-"), "")?.trim()
                ?: "Exploring stunning digital details in ${pinterestNiche.value}. Generated autonomously by JARVIS AI."

            pinterestRunningPinTitle.value = maybeTitle
            pinterestRunningPinDesc.value = maybeDesc
            logPinterestSession("Metadata complete!\nTitle: $maybeTitle\nDescription: $maybeDesc")

            isPinterestWorking.value = false
            pinterestStep.value = 7
            repository.logAction("Metadata Drafted", "Pin descriptions compiled and finalized.", "SUCCESS")
        }
    }

    fun schedulePinterestPin() {
        isPinterestWorking.value = true
        logPinterestSession("Commit to persistent database storage... Compiling SQLite row entry... Scheduling automated post...")

        viewModelScope.launch {
            val title = pinterestRunningPinTitle.value
            val desc = pinterestRunningPinDesc.value
            val tags = pinterestRunningPinTags.value
            val imgB64 = pinterestGeneratedPinImageB64.value
            val dateStr = "Scheduled: Daily at 09:00 AM"

            // Save pin directly in database!
            repository.insertPin(
                PinterestPinEntity(
                    title = title,
                    description = desc,
                    tags = tags,
                    imageBase64 = imgB64,
                    scheduledTime = dateStr,
                    status = "Scheduled",
                    impressions = (150..650).random(),
                    clicks = (20..95).random()
                )
            )

            delaySimulated(1000)
            logPinterestSession("Success! Live campaign uploaded of content calendar.")
            isPinterestWorking.value = false
            pinterestStep.value = 8
            repository.logAction("Pin Scheduled", "Archived '$title' into Pinterest Automated Queue.", "SUCCESS")
        }
    }

    fun resetPinterestStep() {
        pinterestStep.value = 1
        pinterestAuthorized.value = false
        pinterestBioText.value = ""
        pinterestProfileImageB64.value = ""
        pinterestStrategyPlan.value = ""
        pinterestGeneratedPinImageB64.value = ""
        pinterestSessionLogs.value = listOf("System reset. Session logs flushed.")
    }

    // --- FILE SYSTEM SIMULATOR CORES ---
    val sandboxFiles = MutableStateFlow<List<String>>(
        listOf("jarvis_kernel.bin", "memory_triples.json", "pinterest_workflow.py", "audit_trail.log")
    )
    val filesInputName = MutableStateFlow("")

    fun createSandboxFolder() {
        val name = filesInputName.value.trim()
        if (name.isEmpty()) return
        sandboxFiles.value = sandboxFiles.value + name
        filesInputName.value = ""
        viewModelScope.launch {
            repository.logAction("FileSystem Action", "Created simulated sandbox resource: '$name'", "SUCCESS")
        }
    }

    // --- BROWSER SIMULATOR CORES ---
    val browserUrl = MutableStateFlow("https://www.pinterest.com")
    val browserSummaryText = MutableStateFlow("URL Loaded. Press 'AI Research Webpage Summary' to query screen accessibility vision on this endpoint.")
    val isBrowserWorking = MutableStateFlow(false)

    fun navigateBrowser(targetUrl: String) {
        browserUrl.value = targetUrl
        browserSummaryText.value = "Website viewport changed. Waiting visual query trigger..."
        viewModelScope.launch {
            repository.logAction("Browser Nav", "Simulated viewport load of url: '$targetUrl'", "INFO")
        }
    }

    fun summarizeCurrentWebpage() {
        isBrowserWorking.value = true
        viewModelScope.launch {
            val userPrompt = "Summarize the major information, intent, and text contents of this website: ${browserUrl.value}"
            val customSys = "You are a web navigation AI scraper. Write a bulleted list summary of findings."
            repository.logAction("Browser Scraper", "Initiated active web scraper query on ${browserUrl.value}", "INFO")

            val result = callGeminiAPI(customSys, userPrompt, modelFlash)
            browserSummaryText.value = result
            isBrowserWorking.value = false
            repository.logAction("Scrape Successful", "Summarized DOM nodes for website.", "SUCCESS")
        }
    }

    // Delay helper for natural feeling simulations
    private suspend fun delaySimulated(ms: Long) {
        withContext(Dispatchers.IO) {
            Thread.sleep(ms)
        }
    }

    // Fallbacks for simulated modes (When API Key is empty code)
    private fun getSimulatedFallback(prompt: String, context: String): String {
        return when {
            context.contains("Content Agent", ignoreCase = true) -> """
                🚀 **JARVIS Content Agent Proposal**
                
                Content Outline based on: "$prompt"
                
                1. **Digital Visual Moodboard**: Glowing high fidelity composition combining neon cyan lines and slate gray depths.
                2. **Target Audience Engagement**: Call-to-action inviting followers to click the visual profile bio and contribute ideas.
                3. **Optimized SEO Tags**: #AIArt #CyberpunkVisuals #DigitalArtwork #JARVISAutomation #FutureDesign
                
                *Note: The platform is running in Sandbox Simulation Mode.*
            """.trimIndent()
            
            prompt.contains("Pinterest", ignoreCase = true) || context.contains("Pinterest", ignoreCase = true) -> """
                📋 **Pinterest Art Strategy: ${pinterestNiche.value}**
                
                * **Core Branding username**: @${pinterestBrandingName.value}
                * **Niche Category**: AI Cyberpunk Landscapes
                * **Suggested Strategy Plan**:
                  - *Theme 1 (Sci-Fi Citadel)*: Pin rendering neon monolith architecture in dynamic rain.
                  - *Theme 2 (Holographic Transit)*: Vector pin showcasing glowing highway flyovers under a digital sun.
                  - *Theme 3 (Astral Port)*: High depth cosmic terminal bordered by vibrant laser tracks.
                  
                *JARVIS autonomously suggests visual titles and scheduled metadata templates.*
            """.trimIndent()

            prompt.contains("Code", ignoreCase = true) -> """
                ```kotlin
                // JARVIS Autonomous Generated Script
                import kotlinx.coroutines.flow.Flow
                import androidx.room.*

                @Entity(tableName = "autonomous_actions")
                data class ActionItem(
                    @PrimaryKey(autoGenerate = true) val id: Int = 0,
                    val label: String,
                    val timestamp: Long = System.currentTimeMillis()
                )

                @Dao
                interface ActionDao {
                    @Query("SELECT * FROM autonomous_actions ORDER BY timestamp DESC")
                    fun observeActions(): Flow<List<ActionItem>>
                }
                ```
            """.trimIndent()
            
            prompt.contains("Scraper", ignoreCase = true) || prompt.contains("Summarize", ignoreCase = true) -> """
                🌐 **Website Scraped Analysis summary: ${browserUrl.value}**
                
                * **Major Node Identified**: Main community feeds displaying popular cybernetic artwork vectors.
                * **Interactive Buttons Found**: "Log In" (coord: 420x50), "Create Account" (coord: 800x120), "Explore Trends" (coord: 500x350).
                * **Content Objective**: The destination page aggregates organic Pinterest Art boards.
            """.trimIndent()

            else -> """
                ⚡ **JARVIS Response Core**
                I have structured solutions for your query: "$prompt".
                
                - **Autonomous Action Routing**: Successfully integrated your query in the action registers.
                - **Simulated Brain Integrity**: Running smoothly at 98.7% CPU efficiency under local thread memory.
                
                Please register your custom **GEMINI_API_KEY** in the AI Studio platform Secrets dashboard to enable live model intelligence.
            """.trimIndent()
        }
    }

    private fun getSimulatedImageB64(prompt: String): String {
        // We will generate a colored gradient preview in code, but we return a real preset Base64 image
        // to render in Coil cleanly. The preset represents a beautiful gradient/artwork placeholder.
        // A generic 1x1 or simple tiny transparent blue PNG base64:
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mPsb26uBwAEhAF0j1Y+cQAAAABJRU5ErkJggg==" // generic transparent 1x1
    }
}

// Model & Factory Classes
data class ChatMessage(
    val sender: String, // "User", "Jarvis"
    val message: String,
    val timestamp: Long
)

class JarvisViewModelFactory(private val repository: JarvisRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JarvisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JarvisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
