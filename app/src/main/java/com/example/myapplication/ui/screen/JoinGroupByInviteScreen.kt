package com.example.myapplication.ui.screen

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.GroupInvite
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.repository.GroupInviteRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.session.CurrentSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupByInviteScreen(navController: NavHostController) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val inviteRepository = GroupInviteRepository(database.groupInviteDao())
    val memberRepository = GroupMemberRepository(database.groupMemberDao())
    
    var inviteCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val userId = CurrentSession.userIdInt ?: 0
    
    // 从Bitmap解码二维码
    suspend fun decodeQRCodeFromBitmap(bitmap: android.graphics.Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }
    
    // 二维码扫描器（相机扫描）
    val qrCodeLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            val scannedCode = result.contents.trim().uppercase()
            if (scannedCode.length == 6) {
                inviteCode = scannedCode
                errorMessage = null
            } else {
                errorMessage = "扫描的二维码格式不正确，请确保是6位邀请码"
            }
        }
    }
    
    // 从相册选择图片
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                    inputStream?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val decodedText = decodeQRCodeFromBitmap(bitmap)
                            if (decodedText != null) {
                                val scannedCode = decodedText.trim().uppercase()
                                if (scannedCode.length == 6) {
                                    inviteCode = scannedCode
                                    errorMessage = null
                                } else {
                                    errorMessage = "图片中的二维码格式不正确，请确保是6位邀请码"
                                }
                            } else {
                                errorMessage = "无法识别图片中的二维码，请确保图片清晰"
                            }
                        } else {
                            errorMessage = "无法读取图片"
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "读取图片失败: ${e.message}"
                }
            }
        }
    }
    
    // 处理加入小组的逻辑
    fun joinGroup(code: String) {
        if (code.length != 6) {
            errorMessage = "请输入6位邀请码"
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // 查找邀请码
                val invite = inviteRepository.getInviteByCode(code)
                if (invite == null) {
                    errorMessage = "邀请码不存在"
                    isLoading = false
                    return@launch
                }
                
                // 检查是否过期
                if (invite.expiresAt != null && invite.expiresAt < System.currentTimeMillis()) {
                    errorMessage = "邀请码已过期"
                    isLoading = false
                    return@launch
                }
                
                // 检查使用次数
                if (invite.maxUses != null && invite.currentUses >= invite.maxUses) {
                    errorMessage = "邀请码已达到最大使用次数"
                    isLoading = false
                    return@launch
                }
                
                // 检查是否已经加入
                val existingMember = memberRepository.getMember(invite.groupId, userId)
                if (existingMember != null && existingMember.status == MemberStatus.JOINED) {
                    errorMessage = "您已经加入该小组"
                    isLoading = false
                    return@launch
                }
                
                // 添加成员
                if (existingMember == null) {
                    val newMember = GroupMember(
                        groupId = invite.groupId,
                        userId = userId,
                        role = MemberRole.MEMBER,
                        status = MemberStatus.JOINED
                    )
                    memberRepository.insertMember(newMember)
                } else {
                    // 如果之前退出过，重新加入
                    memberRepository.updateMemberStatus(
                        invite.groupId,
                        userId,
                        MemberStatus.JOINED
                    )
                }
                
                // 更新邀请码使用次数
                inviteRepository.incrementUseCount(invite.inviteId)
                
                // 成功，返回并导航到小组详情
                navController.popBackStack()
                navController.navigate("group_detail/${invite.groupId}")
                
            } catch (e: Exception) {
                errorMessage = "加入失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "通过邀请码加入",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 说明卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "邀请码",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "输入邀请码或扫描二维码",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "请输入6位邀请码加入学习小组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 输入框卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { 
                            inviteCode = it.uppercase().take(6) // 限制6位，自动转大写
                        },
                        label = { Text("邀请码") },
                        placeholder = { Text("例如: ABC123") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = "邀请码")
                        },
                        trailingIcon = {
                            if (inviteCode.isNotEmpty()) {
                                IconButton(onClick = { inviteCode = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    // 错误消息
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // 加入按钮
                    Button(
                        onClick = {
                            joinGroup(inviteCode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading && inviteCode.length == 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "加入",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "加入小组",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // 扫码按钮组
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 相机扫描
                OutlinedButton(
                    onClick = {
                        val options = ScanOptions()
                        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        options.setPrompt("将二维码放入扫描框内")
                        options.setCameraId(0)
                        options.setBeepEnabled(false)
                        options.setBarcodeImageEnabled(true)
                        qrCodeLauncher.launch(options)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "扫码",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "相机扫描",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 相册选择
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "相册",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "相册选择",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

