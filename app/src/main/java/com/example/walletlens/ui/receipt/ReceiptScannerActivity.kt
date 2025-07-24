package com.example.walletlens.ui.receipt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.walletlens.R
import com.example.walletlens.databinding.ActivityReceiptScannerBinding
import com.example.walletlens.data.WalletLensDatabase
import com.example.walletlens.data.entity.Transaction
import com.example.walletlens.data.entity.TransactionType
import com.example.walletlens.data.repository.TransactionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import androidx.appcompat.app.AlertDialog
import android.graphics.ImageFormat
import com.google.android.gms.tasks.Tasks
import androidx.camera.core.AspectRatio
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.regex.Pattern

class ReceiptScannerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReceiptScannerBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var photoFile: File? = null
    private var isCameraReady = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for receipt scanning", Toast.LENGTH_LONG).show()
            showCameraPermissionDialog()
        }
    }
    
    companion object {
        private const val TAG = "ReceiptScanner"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        checkCameraPermission()
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.btnCapture.isEnabled = false
        
        binding.btnCapture.setOnClickListener {
            Log.d(TAG, "Capture button clicked")
            if (isCameraReady) {
                takePhoto()
            } else {
                Toast.makeText(this, "Camera not ready. Please wait...", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnGallery.setOnClickListener {
            Log.d(TAG, "Gallery button clicked")
            openGallery()
        }
        
        binding.btnProcess.setOnClickListener {
            Log.d(TAG, "Process button clicked")
            processLastPhoto()
        }
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionDialog()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startCamera() {
        Log.d(TAG, "Starting camera...")
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            binding.tvCameraStatus.text = "No Camera Permission"
            binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
            return
        }
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                if (!cameraProvider.hasCamera(cameraSelector)) {
                    Log.e(TAG, "No back camera available")
                    binding.tvCameraStatus.text = "No Camera Available"
                    binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
                    Toast.makeText(this, "No back camera available on this device", Toast.LENGTH_LONG).show()
                    return@addListener
                }
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }
                
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                
                cameraProvider.unbindAll()
                
                val camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                
                Log.d(TAG, "Camera binding completed successfully")
                binding.tvCameraStatus.text = "Camera Ready - Point at Receipt"
                binding.tvCameraStatus.setBackgroundColor(0xFF4CAF50.toInt())
                
                Handler(Looper.getMainLooper()).postDelayed({
                    isCameraReady = true
                    binding.btnCapture.isEnabled = true
                    Log.d(TAG, "Camera ready for capture")
                }, 500)
                
            } catch (exc: Exception) {
                Log.e(TAG, "Camera setup failed", exc)
                binding.tvCameraStatus.text = "Camera Failed: ${exc.message}"
                binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
                Toast.makeText(this@ReceiptScannerActivity, "Failed to start camera: ${exc.message}", Toast.LENGTH_LONG).show()
                isCameraReady = false
                binding.btnCapture.isEnabled = false
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not ready. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isCameraReady) {
            Toast.makeText(this, "Camera not ready. Please wait...", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        
        this.photoFile = photoFile
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        binding.tvCameraStatus.text = "Capturing Receipt..."
        binding.tvCameraStatus.setBackgroundColor(0xFFFF9800.toInt())
        
        binding.btnCapture.isEnabled = false
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    binding.tvCameraStatus.text = "Capture Failed"
                    binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
                    binding.btnCapture.isEnabled = true
                    Toast.makeText(this@ReceiptScannerActivity, "Failed to capture photo: ${exc.message}", Toast.LENGTH_LONG).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    
                    binding.btnCapture.isEnabled = true
                    
                    if (photoFile.exists() && photoFile.length() > 0) {
                        binding.tvCameraStatus.text = "Receipt Captured!"
                        binding.tvCameraStatus.setBackgroundColor(0xFF4CAF50.toInt())
                        
                        // Automatically start processing
                        processReceipt(photoFile)
                    } else {
                        binding.tvCameraStatus.text = "Capture Failed"
                        binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
                        Toast.makeText(this@ReceiptScannerActivity, "Photo file is empty or missing", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
    
    private fun processReceipt(photoFile: File) {
        binding.tvCameraStatus.text = "Scanning Receipt..."
        binding.tvCameraStatus.setBackgroundColor(0xFFFF9800.toInt())
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load and optimize image for OCR
                val bitmap = loadAndOptimizeImage(photoFile.absolutePath)
                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        showError("Failed to load receipt image")
                    }
                    return@launch
                }
                
                // Perform OCR
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                
                val visionText = withContext(Dispatchers.IO) {
                    try {
                        Tasks.await(recognizer.process(image), 30, java.util.concurrent.TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        throw Exception("OCR processing failed: ${e.message}")
                    }
                }
                
                Log.d(TAG, "OCR completed. Text: ${visionText.text}")
                
                // Extract receipt data
                val receiptData = extractReceiptData(visionText.text)
                
                withContext(Dispatchers.Main) {
                    showReceiptResults(receiptData, photoFile.absolutePath)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt", e)
                withContext(Dispatchers.Main) {
                    showError("Error processing receipt: ${e.message}")
                }
            }
        }
    }
    
    private fun loadAndOptimizeImage(imagePath: String): Bitmap? {
        return try {
            // Load with reduced size for better performance
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            
            val originalBitmap = BitmapFactory.decodeFile(imagePath, options)
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode image: $imagePath")
                return null
            }
            
            // Resize if still too large
            val maxSize = 1024
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            if (width > maxSize || height > maxSize) {
                val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
                val newWidth = (width * scale).toInt()
                val newHeight = (height * scale).toInt()
                
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true).also {
                    if (it != originalBitmap) {
                        originalBitmap.recycle()
                    }
                }
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading and optimizing image: ${e.message}")
            null
        }
    }
    
    private fun extractReceiptData(text: String): ReceiptData {
        return runBlocking(Dispatchers.Default) {
            val lines = text.split("\n")
            var amount = 0.0
            var merchant = ""
            var date = LocalDateTime.now()
            var items = mutableListOf<String>()
            
            Log.d(TAG, "Processing ${lines.size} lines of text")
            
            // Amount extraction patterns
            val amountPatterns = listOf(
                Pattern.compile("TOTAL.*?\\$?([0-9]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("GRAND TOTAL.*?\\$?([0-9]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("AMOUNT.*?\\$?([0-9]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\$([0-9]+\\.[0-9]{2})"),
                Pattern.compile("([0-9]+\\.[0-9]{2})")
            )
            
            // Find amount
            for (line in lines.take(20)) {
                for (pattern in amountPatterns) {
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        val amountStr = matcher.group(1) ?: continue
                        amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            Log.d(TAG, "Found amount: $amount in line: $line")
                            break
                        }
                    }
                }
                if (amount > 0) break
            }
            
            // Extract merchant name (usually in first few lines)
            for (i in 0 until minOf(5, lines.size)) {
                val line = lines[i].trim()
                if (line.isNotEmpty() && 
                    line.length > 3 && 
                    !line.contains("TOTAL") &&
                    !line.contains("RECEIPT") &&
                    !line.contains("THANK") &&
                    !line.contains("WELCOME") &&
                    !line.contains("DATE") &&
                    !line.contains("TIME") &&
                    !Pattern.compile("[0-9]").matcher(line).find()) {
                    merchant = line
                    Log.d(TAG, "Found merchant: $merchant")
                    break
                }
            }
            
            // Extract date
            val datePatterns = listOf(
                Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})"),
                Pattern.compile("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})")
            )
            
            for (line in lines.take(10)) {
                for (pattern in datePatterns) {
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        try {
                            Log.d(TAG, "Found date pattern in line: $line")
                            // Date parsing can be implemented later if needed
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing date: ${e.message}")
                        }
                        break
                    }
                }
            }
            
            // Extract items (lines with prices)
            for (line in lines.take(30)) {
                if (line.contains("$") && !line.contains("TOTAL") && line.length > 5) {
                    items.add(line.trim())
                }
            }
            
            ReceiptData(
                amount = amount,
                merchant = merchant,
                date = date,
                items = items,
                rawText = text
            )
        }
    }
    
    private fun showReceiptResults(receiptData: ReceiptData, imagePath: String) {
        val message = """
            Receipt Scanned Successfully!
            
            Store: ${receiptData.merchant.ifBlank { "Unknown" }}
            Amount: $${String.format("%.2f", receiptData.amount)}
            Items: ${receiptData.items.size} items found
            Date: ${receiptData.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}
            
            Raw Text: ${receiptData.rawText.take(100)}...
            
            What type of transaction is this?
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Receipt Analysis Complete")
            .setMessage(message)
            .setPositiveButton("Add as Expense") { _, _ ->
                addTransaction(receiptData, imagePath, TransactionType.EXPENSE)
            }
            .setNeutralButton("Add as Income") { _, _ ->
                addTransaction(receiptData, imagePath, TransactionType.INCOME)
            }
            .setNegativeButton("Edit Details") { _, _ ->
                showEditDialog(receiptData, imagePath)
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showEditDialog(receiptData: ReceiptData, imagePath: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_receipt, null)
        
        val amountEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextAmount)
        val merchantEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextMerchant)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.categorySpinner)
        val transactionTypeToggle = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.transactionTypeToggle)
        
        amountEditText.setText(String.format("%.2f", receiptData.amount))
        merchantEditText.setText(receiptData.merchant)
        
        // Setup categories based on transaction type
        val expenseCategories = listOf(
            "Food & Dining", "Transportation", "Shopping", "Entertainment", 
            "Utilities", "Healthcare", "Education", "Insurance", "Other"
        )
        
        val incomeCategories = listOf(
            "Salary", "Freelance", "Investment", "Business", 
            "Gift", "Refund", "Other Income"
        )
        
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, expenseCategories)
        categorySpinner.setAdapter(categoryAdapter)
        categorySpinner.setText(suggestCategory(receiptData.merchant), false)
        
        // Handle transaction type toggle
        transactionTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val categories = if (checkedId == R.id.btnIncome) {
                    incomeCategories
                } else {
                    expenseCategories
                }
                
                val newAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
                categorySpinner.setAdapter(newAdapter)
                categorySpinner.setText(categories[0], false)
            }
        }
        
        // Set default to expense
        transactionTypeToggle.check(R.id.btnExpense)
        
        AlertDialog.Builder(this)
            .setTitle("Edit Receipt Details")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: receiptData.amount
                val merchant = merchantEditText.text.toString().ifBlank { receiptData.merchant }
                val category = categorySpinner.text.toString().ifBlank { "Other" }
                val transactionType = if (transactionTypeToggle.checkedButtonId == R.id.btnIncome) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                
                val updatedReceipt = receiptData.copy(
                    amount = amount,
                    merchant = merchant
                )
                
                addTransaction(updatedReceipt, imagePath, transactionType)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addTransaction(receiptData: ReceiptData, imagePath: String, transactionType: TransactionType = TransactionType.EXPENSE) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = WalletLensDatabase.getDatabase(this@ReceiptScannerActivity)
                val transactionRepository = TransactionRepository(database.transactionDao())
                
                val transaction = Transaction(
                    amount = receiptData.amount,
                    description = receiptData.merchant.ifBlank { "Receipt" },
                    category = suggestCategory(receiptData.merchant),
                    type = transactionType,
                    date = receiptData.date,
                    imagePath = imagePath
                )
                
                transactionRepository.insertTransaction(transaction)
                
                withContext(Dispatchers.Main) {
                    binding.tvCameraStatus.text = "Transaction Added!"
                    binding.tvCameraStatus.setBackgroundColor(0xFF4CAF50.toInt())
                    
                    val typeText = if (transactionType == TransactionType.INCOME) "income" else "expense"
                    val message = if (receiptData.amount > 0) {
                        "Added $${String.format("%.2f", receiptData.amount)} $typeText from ${receiptData.merchant}"
                    } else {
                        "Receipt $typeText added successfully!"
                    }
                    
                    Toast.makeText(this@ReceiptScannerActivity, message, Toast.LENGTH_LONG).show()
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 2000)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding transaction", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to save transaction: ${e.message}")
                }
            }
        }
    }
    
    private fun suggestCategory(merchant: String): String {
        val merchantLower = merchant.lowercase()
        return when {
            merchantLower.contains("grocery") || merchantLower.contains("supermarket") || 
            merchantLower.contains("food") || merchantLower.contains("market") -> "Food & Dining"
            merchantLower.contains("restaurant") || merchantLower.contains("cafe") || 
            merchantLower.contains("pizza") || merchantLower.contains("burger") -> "Food & Dining"
            merchantLower.contains("gas") || merchantLower.contains("fuel") || 
            merchantLower.contains("uber") || merchantLower.contains("taxi") -> "Transportation"
            merchantLower.contains("movie") || merchantLower.contains("theater") || 
            merchantLower.contains("game") || merchantLower.contains("entertainment") -> "Entertainment"
            merchantLower.contains("mall") || merchantLower.contains("store") || 
            merchantLower.contains("shop") || merchantLower.contains("clothing") -> "Shopping"
            merchantLower.contains("pharmacy") || merchantLower.contains("medical") || 
            merchantLower.contains("doctor") || merchantLower.contains("hospital") -> "Healthcare"
            merchantLower.contains("electric") || merchantLower.contains("water") || 
            merchantLower.contains("internet") || merchantLower.contains("phone") -> "Utilities"
            merchantLower.contains("school") || merchantLower.contains("university") || 
            merchantLower.contains("book") || merchantLower.contains("course") -> "Education"
            else -> "Other"
        }
    }
    
    private fun openGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening gallery", e)
            Toast.makeText(this, "Failed to open gallery: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                binding.tvCameraStatus.text = "Processing Image..."
                binding.tvCameraStatus.setBackgroundColor(0xFFFF9800.toInt())
                
                photoFile = createTempFileFromUri(it)
                if (photoFile?.exists() == true) {
                    processReceipt(photoFile!!)
                } else {
                    binding.tvCameraStatus.text = "Failed to Process"
                    binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing gallery image", e)
                binding.tvCameraStatus.text = "Processing Failed"
                binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
                Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun createTempFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return null
            }
            
            val tempFile = File.createTempFile("receipt_", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            
            try {
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                inputStream.close()
                outputStream.close()
            }
            
            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile
            } else {
                Log.e(TAG, "Temp file creation failed or file is empty")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file from URI: ${e.message}")
            null
        }
    }
    
    private fun processLastPhoto() {
        photoFile?.let { file ->
            if (file.exists()) {
                processReceipt(file)
            } else {
                Toast.makeText(this, "No photo available to process", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No photo available to process", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showError(message: String) {
        binding.tvCameraStatus.text = "Error: $message"
        binding.tvCameraStatus.setBackgroundColor(0xFFF44336.toInt())
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showCameraPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app needs camera access to scan receipts. Please grant camera permission to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Camera permission is required for receipt scanning", Toast.LENGTH_LONG).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraExecutor.shutdown()
            imageCapture = null
            isCameraReady = false
            System.gc()
            
            photoFile?.let { file ->
                if (file.exists()) {
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting temp file: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }
    
    private val outputDirectory: File by lazy {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
}

data class ReceiptData(
    val amount: Double,
    val merchant: String,
    val date: LocalDateTime,
    val items: List<String>,
    val rawText: String
) 