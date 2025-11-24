package com.example.myapplication.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.model.GroupInvite
import com.example.myapplication.data.model.GroupMember
import com.example.myapplication.data.model.MemberRole
import com.example.myapplication.data.model.MemberStatus
import com.example.myapplication.data.repository.GroupInviteRepository
import com.example.myapplication.data.repository.GroupMemberRepository
import com.example.myapplication.session.CurrentSession
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val extracted = Regex("([A-Za-z0-9]{6})")
                .find(result.contents.uppercase())
                ?.value
            if (extracted != null) {
                inviteCode = extracted
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    handleJoin(inviteCode, inviteRepository, memberRepository, userId, navController) { msg ->
                        errorMessage = msg
                    }
                    isLoading = false
                }
            } else {
                errorMessage = "二维码中未找到有效邀请码"
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
                            if (inviteCode.length != 6) {
                                errorMessage = "请输入6位邀请码"
                                return@Button
                            }
                            
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                handleJoin(inviteCode, inviteRepository, memberRepository, userId, navController) { msg ->
                                    errorMessage = msg
                                }
                                isLoading = false
                            }
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
            
            OutlinedButton(
                onClick = {
                    val options = ScanOptions().apply {
                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        setPrompt("请扫描学习小组二维码")
                        setBeepEnabled(false)
                        setOrientationLocked(false)
                    }
                    scanLauncher.launch(options)
                },
                modifier = Modifier
                    .fillMaxWidth()
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
                    text = "扫描二维码",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private suspend fun handleJoin(
    code: String,
    inviteRepository: GroupInviteRepository,
    memberRepository: GroupMemberRepository,
    userId: Int,
    navController: NavHostController,
    onError: (String?) -> Unit
) {
    try {
        val invite = inviteRepository.getInviteByCode(code)
        if (invite == null) {
            onError("邀请码不存在")
            return
        }

        if (invite.expiresAt != null && invite.expiresAt < System.currentTimeMillis()) {
            onError("邀请码已过期")
            return
        }

        if (invite.maxUses != null && invite.currentUses >= invite.maxUses) {
            onError("邀请码已达到最大使用次数")
            return
        }

        val existingMember = memberRepository.getMember(invite.groupId, userId)
        if (existingMember != null && existingMember.status == MemberStatus.JOINED) {
            onError("您已经加入该小组")
            return
        }

        if (existingMember == null) {
            val newMember = GroupMember(
                groupId = invite.groupId,
                userId = userId,
                role = MemberRole.MEMBER,
                status = MemberStatus.JOINED
            )
            memberRepository.insertMember(newMember)
        } else {
            memberRepository.updateMemberStatus(
                invite.groupId,
                userId,
                MemberStatus.JOINED
            )
        }

        inviteRepository.incrementUseCount(invite.inviteId)

        navController.popBackStack()
        navController.navigate("group_detail/${invite.groupId}")
        onError(null)
    } catch (e: Exception) {
        onError("加入失败: ${e.message}")
    }
}

