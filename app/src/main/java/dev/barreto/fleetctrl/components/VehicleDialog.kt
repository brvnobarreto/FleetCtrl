package dev.barreto.fleetctrl.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dev.barreto.fleetctrl.R
import dev.barreto.fleetctrl.data.database.entities.Vehicle
import dev.barreto.fleetctrl.data.remote.VehicleImageCandidate
import dev.barreto.fleetctrl.data.remote.VehicleImageFetcher
import dev.barreto.fleetctrl.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDialog(
    vehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSave: suspend (Vehicle) -> Boolean,
    modifier: Modifier = Modifier,
    currentOrgId: String? = null
) {
    val vehicleId = vehicle?.id ?: 0L
    var vehicleNumber by remember(vehicleId) { mutableStateOf(vehicle?.vehicleNumber ?: "") }
    var model by remember(vehicleId) { mutableStateOf(vehicle?.model ?: "") }
    var brand by remember(vehicleId) { mutableStateOf(vehicle?.brand ?: "") }
    var year by remember(vehicleId) { mutableStateOf(vehicle?.year?.toString() ?: "") }
    var plate by remember(vehicleId) { mutableStateOf(vehicle?.plate ?: "") }
    var driver by remember(vehicleId) { mutableStateOf(vehicle?.driver ?: "") }
    var showInDiary by remember(vehicleId) { mutableStateOf(vehicle?.showInDiary ?: true) }
    var photoPath by remember(vehicleId) { mutableStateOf(vehicle?.photoPath) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showUrlEntryDialog by remember { mutableStateOf(false) }
    var showDriveLinkDialog by remember { mutableStateOf(false) }
    var pendingDriveUri by remember { mutableStateOf<Uri?>(null) }
    val currentYear = LocalDateTime.now().year
    val yearRange = (1900..currentYear + 1)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageFetcher = remember { VehicleImageFetcher() }
    var isFetchingImage by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var imageSuggestions by remember { mutableStateOf<List<VehicleImageCandidate>>(emptyList()) }
    var showSuggestionDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val applyCandidate: (VehicleImageCandidate) -> Unit = { candidate ->
        photoPath = candidate.imageUrl
        fetchError = null
        imageSuggestions = emptyList()
        showSuggestionDialog = false
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        Log.d("VehicleDialog", "=== GALLERY LAUNCHER CALLED ===")
        Log.d("VehicleDialog", "Gallery launcher called with URI: $uri")
        uri?.let {
            try {
                Log.d("VehicleDialog", "URI scheme: ${it.scheme}, authority: ${it.authority}, path: ${it.path}")
                
                // Persist permission for long-term access (Drive and other providers)
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                
                // Check if the URI is from Google Drive
                val driveUrl = extractDriveUrlFromUri(context, it)
                Log.d("VehicleDialog", "Extracted Drive URL: $driveUrl")
                if (driveUrl != null) {
                    // If it's from Drive, use the direct URL instead of copying locally
                    val directUrl = mapDriveUrlToDirect(driveUrl)
                    Log.d("VehicleDialog", "Mapped to direct URL: $directUrl")
                    photoPath = directUrl
                    fetchError = null
                } else {
                    // Check if it might be from Drive by checking the display name or other properties
                    val isFromDrive = isUriFromGoogleDrive(context, it)
                    Log.d("VehicleDialog", "Is from Drive: $isFromDrive")
                    if (isFromDrive) {
                        // If we suspect it's from Drive but couldn't extract the URL,
                        // show a dialog asking for the Drive link
                        Log.w("VehicleDialog", "Detected Drive image but couldn't extract URL, asking for link")
                        pendingDriveUri = it
                        showDriveLinkDialog = true
                        return@let
                    }
                    // For other sources, copy the file locally as before
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fileName = "vehicle_${System.currentTimeMillis()}.jpg"
                    val file = File(context.filesDir, "vehicle_images")
                    if (!file.exists()) file.mkdirs()
                    val imageFile = File(file, fileName)
                    
                    inputStream?.use { input ->
                        imageFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    photoPath = imageFile.absolutePath
                    fetchError = null
                }
            } catch (e: Exception) {
                fetchError = context.getString(R.string.vehicle_image_copy_error)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vehicle == null) stringResource(R.string.vehicle_dialog_add_title) else stringResource(R.string.vehicle_dialog_edit_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                VehiclePhotoSection(photoPath = photoPath, onPhotoClick = { showImagePicker = true })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (brand.isBlank() && model.isBlank()) {
                                fetchError = context.getString(R.string.vehicle_image_missing_data)
                                return@OutlinedButton
                            }
                            fetchError = null
                            isFetchingImage = true
                            val yearInt = year.toIntOrNull()
                            
                            scope.launch {
                                try {
                                    val candidates = imageFetcher.fetchCandidates(brand.trim(), model.trim(), yearInt)
                                    when {
                                        candidates.isEmpty() -> fetchError = context.getString(R.string.vehicle_image_no_results)
                                        candidates.size == 1 -> applyCandidate(candidates.first())
                                        else -> {
                                            imageSuggestions = candidates
                                            showSuggestionDialog = true
                                        }
                                    }
                                } catch (_: Exception) {
                                    fetchError = context.getString(R.string.vehicle_image_fetch_error)
                                } finally {
                                    isFetchingImage = false
                                }
                            }
                        },
                        enabled = !isFetchingImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isFetchingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.vehicle_detail_fetching_image))
                        } else {
                            Icon(Icons.Default.ImageSearch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.vehicle_detail_fetch_image))
                        }
                    }
                    OutlinedButton(onClick = { showImagePicker = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.vehicle_dialog_pick_photo))
                    }
                }
                fetchError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(value = vehicleNumber, onValueChange = { vehicleNumber = it }, label = { Text(stringResource(R.string.vehicle_dialog_vehicle_number_label)) }, leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = model, onValueChange = { model = it; fetchError = null }, label = { Text(stringResource(R.string.vehicle_dialog_model_label)) }, leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = brand, onValueChange = { brand = it; fetchError = null }, label = { Text(stringResource(R.string.vehicle_dialog_brand_label)) }, leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text(stringResource(R.string.vehicle_dialog_year_label)) }, leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = true, trailingIcon = { IconButton(onClick = { showYearPicker = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.cd_year_select)) } })
                OutlinedTextField(value = plate, onValueChange = { plate = it.uppercase() }, label = { Text(stringResource(R.string.vehicle_dialog_plate_label)) }, leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = driver, onValueChange = { driver = it }, label = { Text(stringResource(R.string.vehicle_dialog_driver_label)) }, leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showInDiary, onCheckedChange = { showInDiary = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.vehicle_dialog_show_diary), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val yearInt = year.toIntOrNull() ?: currentYear
                    val newVehicle = Vehicle(
                        id = vehicle?.id ?: 0,
                        vehicleNumber = vehicleNumber,
                        model = model,
                        brand = brand,
                        year = yearInt,
                        color = vehicle?.color ?: "Não especificada",
                        driver = driver,
                        plate = plate,
                        showInDiary = showInDiary,
                        photoPath = photoPath ?: vehicle?.photoPath,
                        engineType = vehicle?.engineType ?: "Gasolina",
                        fuelCapacity = vehicle?.fuelCapacity ?: 50.0,
                        averageConsumption = vehicle?.averageConsumption ?: 12.0,
                        isActive = vehicle?.isActive ?: true,
                        currentMileage = vehicle?.currentMileage ?: 0,
                        lastMaintenanceMileage = vehicle?.lastMaintenanceMileage ?: 0,
                        createdAt = vehicle?.createdAt ?: LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                        notes = null,
                        organizationId = vehicle?.organizationId ?: currentOrgId
                    )
                    scope.launch {
                        isSaving = true
                        try {
                            if (onSave(newVehicle)) {
                                onDismiss()
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && vehicleNumber.isNotBlank() && model.isNotBlank() && year.isNotBlank() && plate.isNotBlank() && driver.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.vehicle_dialog_saving))
                } else {
                    Text(stringResource(R.string.vehicle_dialog_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.vehicle_dialog_cancel))
            }
        },
        modifier = modifier
    )

    if (showSuggestionDialog) {
        ImageSuggestionDialog(suggestions = imageSuggestions, onSelect = { applyCandidate(it) }, onDismiss = { showSuggestionDialog = false })
    }
    
    if (showYearPicker) {
        YearPickerDialog(selectedYear = year.toIntOrNull() ?: currentYear, yearRange = yearRange, onYearSelected = { selectedYear -> year = selectedYear.toString(); showYearPicker = false }, onDismiss = { showYearPicker = false })
    }
    
    if (showImagePicker) {
        ImagePickerDialog(
            onGalleryClick = { galleryLauncher.launch(arrayOf("image/*")); showImagePicker = false },
            onCameraClick = { galleryLauncher.launch(arrayOf("image/*")); showImagePicker = false },
            onUrlClick = { showUrlEntryDialog = true; showImagePicker = false },
            onDismiss = { showImagePicker = false }
        )
    }

    if (showUrlEntryDialog) {
        UrlEntryDialog(
            onConfirm = { url ->
                // Converte link compartilhável do Drive em link direto (usercontent) e salva como URL
                val direct = mapDriveUrlToDirect(url.trim())
                photoPath = direct
                fetchError = null
                showUrlEntryDialog = false
            },
            onDismiss = { showUrlEntryDialog = false }
        )
    }

    if (showDriveLinkDialog) {
        AlertDialog(
            onDismissRequest = { showDriveLinkDialog = false },
            title = { Text("Imagem do Google Drive") },
            text = { 
                Text("Detectamos que você selecionou uma imagem do Google Drive, mas não conseguimos extrair o link automaticamente. Por favor, cole o link compartilhável da imagem do Drive abaixo:") 
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showDriveLinkDialog = false
                        showUrlEntryDialog = true
                    }
                ) {
                    Text("Colar Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDriveLinkDialog = false
                        // Copia a imagem localmente como fallback
                        pendingDriveUri?.let { uri ->
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val fileName = "vehicle_${System.currentTimeMillis()}.jpg"
                                val file = File(context.filesDir, "vehicle_images")
                                if (!file.exists()) file.mkdirs()
                                val imageFile = File(file, fileName)
                                
                                inputStream?.use { input ->
                                    imageFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                
                                photoPath = imageFile.absolutePath
                                fetchError = null
                            } catch (e: Exception) {
                                fetchError = context.getString(R.string.vehicle_image_copy_error)
                            }
                        }
                        pendingDriveUri = null
                    }
                ) {
                    Text("Usar Local")
                }
            }
        )
    }
}

@Composable
private fun VehiclePhotoSection(photoPath: String?, onPhotoClick: () -> Unit) {
    val TAG = "VehiclePhotoSection"
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(photoPath) {
        if (photoPath?.contains("drive.google.com") == true) {
            // Deixa o Coil fazer o carregamento via URL direta mapeada; evita baixar HTML de confirmação
            return@LaunchedEffect
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().clickable(onClick = onPhotoClick)) {
        Card(modifier = Modifier.size(120.dp).padding(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    hasError -> Icon(Icons.Default.BrokenImage, contentDescription = stringResource(R.string.cd_vehicle_photo), modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    bitmap != null -> Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = stringResource(R.string.cd_vehicle_photo), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else -> {
                        val imageModel = remember(photoPath) {
                            when {
                                photoPath.isNullOrBlank() -> null
                                photoPath.startsWith("http", ignoreCase = true) -> mapDriveUrlToDirect(photoPath)
                                photoPath.startsWith("/") -> File(photoPath)
                                else -> try { Uri.parse(photoPath) } catch (e: Exception) { null }
                            }
                        }

                        if (imageModel != null) {
                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .allowHardware(false)
                                    .addHeader("User-Agent", "Mozilla/5.0")
                                    .build()
                            )
                            when (val state = painter.state) {
                                is AsyncImagePainter.State.Loading -> CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                is AsyncImagePainter.State.Error -> {
                                    Log.e(TAG, "Coil loading error: ${state.result.throwable}")
                                    Icon(Icons.Default.BrokenImage, contentDescription = null)
                                }
                                else -> Image(painter = painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(R.string.cd_add_photo), modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadBitmapFromUrl(url: String): android.graphics.Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            var currentUrl = url
            var redirects = 0
            while (redirects < 5) {
                val conn = URL(currentUrl).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Chrome/118 Safari/537.36")
                conn.setRequestProperty("Accept", "image/*;q=0.9,*/*;q=0.8")
                conn.connect()
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirects++
                        conn.disconnect()
                        continue
                    }
                }
                conn.inputStream.use { input ->
                    return@withContext BitmapFactory.decodeStream(input)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun extractDriveFileId(url: String): String? {
    return try {
        when {
            url.contains("/file/d/") -> url.substringAfter("/file/d/").substringBefore('/')
            url.contains("/d/") -> url.substringAfter("/d/").substringBefore('/')
            url.contains("open?id=") -> url.substringAfter("open?id=").substringBefore('&')
            else -> null
        }
    } catch (_: Exception) { null }
}

private fun extractDriveUrlFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        Log.d("VehicleDialog", "extractDriveUrlFromUri called with URI: $uri")
        
        // Method 1: Check if the URI is from Google Drive by authority
        val authority = uri.authority
        Log.d("VehicleDialog", "URI authority: $authority")
        if (authority?.contains("drive.google.com") == true) {
            Log.d("VehicleDialog", "Drive authority detected")
            // Try to extract the file ID from the URI path
            val path = uri.path
            Log.d("VehicleDialog", "URI path: $path")
            val fileId = when {
                path?.contains("/file/d/") == true -> path.substringAfter("/file/d/").substringBefore('/')
                path?.contains("/d/") == true -> path.substringAfter("/d/").substringBefore('/')
                else -> null
            }
            Log.d("VehicleDialog", "Extracted file ID: $fileId")
            
            if (fileId != null) {
                // Construct the shareable Drive URL
                val driveUrl = "https://drive.google.com/file/d/$fileId/view"
                Log.d("VehicleDialog", "Constructed Drive URL: $driveUrl")
                return driveUrl
            }
        }
        
        // Method 2: For content URIs, try to get the original URL from content resolver
        Log.d("VehicleDialog", "Trying content resolver query")
        val cursor = context.contentResolver.query(uri, arrayOf("_display_name", "_data"), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(0)
                val data = it.getString(1)
                Log.d("VehicleDialog", "Content resolver - displayName: $displayName, data: $data")
                if (data?.contains("drive.google.com") == true) {
                    Log.d("VehicleDialog", "Found Drive URL in data: $data")
                    return data
                }
            }
        }
        
        // Method 3: Check if the URI scheme suggests it might be from Drive
        if (uri.scheme == "content" && uri.toString().contains("drive")) {
            Log.d("VehicleDialog", "Content URI with 'drive' detected")
            // Try to get more information about the content
            val cursor2 = context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)
            cursor2?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(0)
                    // If the display name suggests it's from Drive, we might need to handle it differently
                    Log.d("VehicleDialog", "Content URI from Drive detected, display name: $displayName")
                }
            }
        }
        
        Log.d("VehicleDialog", "No Drive URL found")
        null
    } catch (e: Exception) {
        Log.w("VehicleDialog", "Failed to extract Drive URL from URI: ${e.message}")
        null
    }
}

private fun isUriFromGoogleDrive(context: android.content.Context, uri: Uri): Boolean {
    return try {
        // Check if the URI authority indicates Google Drive
        val authority = uri.authority
        if (authority?.contains("docs.storage") == true || authority?.contains("drive") == true) {
            Log.d("VehicleDialog", "Drive detected by authority: $authority")
            return true
        }
        
        // Check various indicators that this might be from Google Drive
        val cursor = context.contentResolver.query(uri, arrayOf("_display_name", "_data", "mime_type"), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(0)
                val data = it.getString(1)
                val mimeType = it.getString(2)
                
                // Check if any of these suggest it's from Drive
                val isFromDrive = displayName?.contains("drive", ignoreCase = true) == true ||
                        data?.contains("drive.google.com") == true ||
                        data?.contains("drive") == true ||
                        mimeType?.contains("drive") == true
                
                Log.d("VehicleDialog", "Drive detection - displayName: $displayName, data: $data, mimeType: $mimeType, isFromDrive: $isFromDrive")
                return isFromDrive
            }
        }
        false
    } catch (e: Exception) {
        Log.w("VehicleDialog", "Failed to check if URI is from Drive: ${e.message}")
        false
    }
}

private fun resolveGoogleDriveDirectUrl(url: String): String? {
    val id = extractDriveFileId(url) ?: return null
    // Direct content endpoint (works for many public files)
    return "https://drive.google.com/uc?export=download&id=$id"
}

private fun mapDriveUrlToDirect(url: String): String {
    return if (url.contains("drive.google.com")) {
        // Preferir domínio usercontent para conteúdo direto
        val id = extractDriveFileId(url)
        if (id != null) "https://drive.usercontent.google.com/uc?export=download&id=$id" else url
    } else url
}

private suspend fun importImageFromUrl(context: android.content.Context, url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val finalUrl = if (url.contains("drive.google.com")) resolveGoogleDriveDirectUrl(url) ?: url else url
            var currentUrl = finalUrl
            var redirects = 0
            var connection: HttpURLConnection
            while (true) {
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Chrome/118 Safari/537.36")
                connection.connect()
                val code = connection.responseCode
                if (code in 300..399 && redirects < 5) {
                    val loc = connection.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        currentUrl = loc
                        redirects++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }
            val fileName = "vehicle_${System.currentTimeMillis()}.jpg"
            val dir = File(context.filesDir, "vehicle_images")
            if (!dir.exists()) dir.mkdirs()
            val imageFile = File(dir, fileName)
            connection.inputStream.use { input ->
                imageFile.outputStream().use { out -> input.copyTo(out) }
            }
            imageFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun ImageSuggestionDialog(suggestions: List<VehicleImageCandidate>, onSelect: (VehicleImageCandidate) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.vehicle_image_select_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(stringResource(R.string.vehicle_image_select_subtitle), style = MaterialTheme.typography.bodyMedium); LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(suggestions) { candidate -> ImageSuggestionItem(candidate = candidate, onSelect = { onSelect(candidate) }) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } })
}

@Composable
private fun ImageSuggestionItem(candidate: VehicleImageCandidate, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable { onSelect() }, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val painter = rememberAsyncImagePainter(model = candidate.previewUrl)
            Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    is AsyncImagePainter.State.Error -> Icon(Icons.Default.BrokenImage, contentDescription = candidate.title, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                    else -> Image(painter = painter, contentDescription = candidate.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = candidate.title ?: stringResource(R.string.vehicle_image_no_title), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.vehicle_image_source, candidate.providerName), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                candidate.attribution?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun YearPickerDialog(selectedYear: Int, yearRange: IntRange, onYearSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.dialog_select_year)) }, text = { Column { Text(stringResource(R.string.dialog_year, selectedYear)); Slider(value = selectedYear.toFloat(), onValueChange = { onYearSelected(it.toInt()) }, valueRange = yearRange.first.toFloat()..yearRange.last.toFloat(), steps = yearRange.last - yearRange.first - 1) } }, confirmButton = { Button(onClick = { onYearSelected(selectedYear) }) { Text(stringResource(R.string.dialog_confirm)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } })
}

@Composable
private fun ImagePickerDialog(onGalleryClick: () -> Unit, onCameraClick: () -> Unit, onUrlClick: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.dialog_select_photo)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(stringResource(R.string.dialog_photo_prompt)); OutlinedButton(onClick = onGalleryClick, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PhotoLibrary, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.dialog_gallery)) }; OutlinedButton(onClick = onUrlClick, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AddLink, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.dialog_url)) } } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } }, confirmButton = {})
}

@Composable
private fun UrlEntryDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    val isUrlValid = remember(url) { url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.dialog_url_title)) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(R.string.dialog_url_prompt)); OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.dialog_url_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth()); Text(text = stringResource(R.string.dialog_url_cloud_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { Button(onClick = { onConfirm(url) }, enabled = isUrlValid) { Text(stringResource(R.string.dialog_confirm)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } })
}
