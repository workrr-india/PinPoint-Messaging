package com.example.pinpoint.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.pinpoint.model.Contact
import com.example.pinpoint.model.ContactInvitation
import com.example.pinpoint.model.GroupConversation
import com.example.pinpoint.model.ProfilePictureVisibility
import com.example.pinpoint.model.UserProfile
import com.example.pinpoint.ui.components.DeliveryTickBadge
import com.example.pinpoint.ui.components.QrCodeCanvas
import com.example.pinpoint.viewmodel.ChatViewModel
import com.example.pinpoint.viewmodel.ConversationPreview
import com.example.pinpoint.viewmodel.MainTab
import com.example.ui.theme.*
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val conversations by viewModel.conversationItems.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val invitations by viewModel.invitations.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isSearchActive by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Intercept back gestures when drawer or search is active to avoid unhandled WindowOnBackDispatcher cancellations
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        viewModel.setSearchQuery("")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(310.dp)
            ) {
                // BBM-style Deep Teal Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BbmTealPrimary)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        viewModel.setShowPicturePrivacyDialog(true)
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val avatar = userProfile.avatarUri
                                if (avatar != null && !avatar.startsWith("preset://")) {
                                    AsyncImage(
                                        model = avatar,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ppm_logo),
                                        contentDescription = "PinPoint Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile.displayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = userProfile.statusMessage,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        viewModel.setShowPicturePrivacyDialog(true)
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                ) {
                                    Icon(
                                        imageVector = when (userProfile.pictureVisibility) {
                                            ProfilePictureVisibility.PUBLIC -> Icons.Default.Public
                                            ProfilePictureVisibility.CONTACTS_ONLY -> Icons.Default.People
                                            ProfilePictureVisibility.PRIVATE_SELECTED -> Icons.Default.Lock
                                        },
                                        contentDescription = null,
                                        tint = PinCyanAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (userProfile.pictureVisibility) {
                                            ProfilePictureVisibility.PUBLIC -> "Photo: Public"
                                            ProfilePictureVisibility.CONTACTS_ONLY -> "Photo: Contacts Only"
                                            ProfilePictureVisibility.PRIVATE_SELECTED -> "Photo: Private (${userProfile.allowedPersonPins.size} persons, ${userProfile.allowedGroupIds.size} grp)"
                                        },
                                        fontSize = 11.sp,
                                        color = PinCyanAccent,
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PIN Badge with Copy Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PinPoint PIN", userProfile.pin))
                                    Toast.makeText(context, "PIN ${userProfile.pin} copied!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PIN: ${userProfile.pin}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Chats") },
                    selected = selectedTab == MainTab.CHATS,
                    onClick = {
                        viewModel.selectTab(MainTab.CHATS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Feeds & Updates") },
                    selected = selectedTab == MainTab.FEEDS,
                    onClick = {
                        viewModel.selectTab(MainTab.FEEDS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Contacts (${contacts.size})") },
                    selected = selectedTab == MainTab.CONTACTS,
                    onClick = {
                        viewModel.selectTab(MainTab.CONTACTS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Group, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Groups (${groups.size})") },
                    selected = selectedTab == MainTab.GROUPS,
                    onClick = {
                        viewModel.selectTab(MainTab.GROUPS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("My PIN & Barcode") },
                    selected = selectedTab == MainTab.PROFILE,
                    onClick = {
                        viewModel.selectTab(MainTab.PROFILE)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Profile Picture & Privacy") },
                    selected = false,
                    onClick = {
                        viewModel.setShowPicturePrivacyDialog(true)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Invite by PIN") },
                    selected = false,
                    onClick = {
                        viewModel.setShowAddContactDialog(true)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Create Group") },
                    selected = false,
                    onClick = {
                        viewModel.setShowCreateGroupDialog(true)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Groups, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Enterprise Secure Bridge") },
                    selected = false,
                    onClick = {
                        viewModel.startConferenceCall("SecOps Enterprise Bridge")
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Policy, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Legal & Privacy Policy") },
                    selected = false,
                    onClick = {
                        viewModel.setShowLegalCenter(true)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Account & Data Deletion") },
                    selected = false,
                    onClick = {
                        viewModel.setShowLegalCenter(true)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(color = Color(0xFFEEEEEE))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = BbmTealPrimary) },
                    label = { Text("Generate New PIN", color = BbmTealPrimary, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        viewModel.setupNewUserAccount(userProfile.displayName, userProfile.statusMessage)
                        Toast.makeText(context, "New verified unique PIN assigned!", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mandatory Powered by Workrr.in Hyperlink Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                uriHandler.openUri("https://workrr.in")
                            } catch (_: Exception) {
                                Toast.makeText(context, "Opening workrr.in", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("drawer_workrr_footer"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Powered by ",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "Workrr.in",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BbmTealPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open workrr.in",
                        tint = BbmTealPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BbmTealPrimary)
                        .statusBarsPadding()
                ) {
                    // Authentic BBM Deep Teal Top Bar (Screen 3)
                    TopAppBar(
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    MainTab.CHATS -> "Chats"
                                    MainTab.FEEDS -> "Feeds"
                                    MainTab.CONTACTS -> "Contacts"
                                    MainTab.GROUPS -> "Groups"
                                    MainTab.PROFILE -> "My PIN"
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                modifier = Modifier.testTag("action_hamburger_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu Drawer",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { isSearchActive = !isSearchActive },
                                modifier = Modifier.testTag("action_search_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.testTag("action_overflow_menu")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Invite Contact by PIN") },
                                        leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.setShowAddContactDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Create New Group") },
                                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.setShowCreateGroupDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Enterprise Conference Bridge") },
                                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.startConferenceCall("SecOps Enterprise Bridge")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("My Profile & PIN Barcode") },
                                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.selectTab(MainTab.PROFILE)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Profile Picture & Privacy") },
                                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BbmTealPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.setShowPicturePrivacyDialog(true)
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Legal & Privacy Policy") },
                                        leadingIcon = { Icon(Icons.Default.Policy, contentDescription = null, tint = BbmTealPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.setShowLegalCenter(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Generate New PIN", color = BbmTealPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = BbmTealPrimary) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.setupNewUserAccount(userProfile.displayName, userProfile.statusMessage)
                                            Toast.makeText(context, "New verified unique PIN assigned!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BbmTealPrimary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )

                    // Expandable Search Bar
                    if (isSearchActive) {
                        Surface(
                            color = BbmTealDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search chats, PINs, contacts...", color = Color.White.copy(alpha = 0.6f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (searchQuery.isNotEmpty()) viewModel.setSearchQuery("")
                                        else isSearchActive = false
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Close Search", tint = Color.White)
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("search_input")
                            )
                        }
                    }

                    // 4 Iconic BBM Icon Tabs (Screen 3)
                    val tabList = listOf(
                        MainTab.CHATS to Icons.AutoMirrored.Filled.Chat,
                        MainTab.FEEDS to Icons.Default.Sync,
                        MainTab.CONTACTS to Icons.Default.Person,
                        MainTab.GROUPS to Icons.Default.Group
                    )

                    val currentTabIndex = when (selectedTab) {
                        MainTab.CHATS -> 0
                        MainTab.FEEDS -> 1
                        MainTab.CONTACTS -> 2
                        MainTab.GROUPS -> 3
                        MainTab.PROFILE -> 0
                    }

                    TabRow(
                        selectedTabIndex = currentTabIndex,
                        containerColor = BbmTealPrimary,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (currentTabIndex in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTabIndex]),
                                    height = 3.dp,
                                    color = Color.White
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabList.forEachIndexed { index, (tab, icon) ->
                            val isSelected = currentTabIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tab.name,
                                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                // BBM Round Floating Action Button in deep teal with chat bubble icon (Screen 3)
                if (selectedTab == MainTab.CHATS) {
                    FloatingActionButton(
                        onClick = {
                            // Quick start chat with Gideon Smith or open contact picker
                            val firstContact = contacts.firstOrNull()
                            if (firstContact != null) {
                                viewModel.openConversation(firstContact.pin)
                            } else {
                                viewModel.setShowAddContactDialog(true)
                            }
                        },
                        containerColor = BbmTealFab,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("fab_start_chat")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Start Chat",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White)
            ) {
                when (selectedTab) {
                    MainTab.CHATS -> ChatsTabContent(
                        conversations = conversations,
                        onOpenConversation = { viewModel.openConversation(it) }
                    )
                    MainTab.FEEDS -> FeedsTabContent(
                        onOpenChat = { pin -> viewModel.openConversation(pin) }
                    )
                    MainTab.CONTACTS -> ContactsTabContent(
                        contacts = contacts,
                        invitations = invitations,
                        onOpenContact = { viewModel.openConversation(it.pin) },
                        onAcceptInvite = { viewModel.acceptInvitation(it) },
                        onRejectInvite = { viewModel.rejectInvitation(it) },
                        onAddNewContact = { viewModel.setShowAddContactDialog(true) }
                    )
                    MainTab.GROUPS -> GroupsTabContent(
                        groups = groups,
                        onOpenGroup = { viewModel.openConversation(it.id) },
                        onCreateGroup = { viewModel.setShowCreateGroupDialog(true) }
                    )
                    MainTab.PROFILE -> ProfileTabContent(
                        userProfile = userProfile,
                        onUpdateName = { viewModel.updateDisplayName(it) },
                        onUpdateStatus = { viewModel.updateStatusMessage(it) },
                        onSetupNewPin = {
                            viewModel.setupNewUserAccount(userProfile.displayName, userProfile.statusMessage)
                        },
                        onOpenLegalCenter = { viewModel.setShowLegalCenter(true) },
                        onOpenPicturePrivacy = { viewModel.setShowPicturePrivacyDialog(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsTabContent(
    conversations: List<ConversationPreview>,
    onOpenConversation: (String) -> Unit
) {
    if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No conversations found.",
                color = Color(0xFF757575),
                fontSize = 15.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(conversations, key = { it.id }) { item ->
                ConversationItemRow(item = item, onClick = { onOpenConversation(item.id) })
                HorizontalDivider(
                    color = Color(0xFFE0E0E0),
                    thickness = 0.6.dp
                )
            }
        }
    }
}

@Composable
private fun ConversationItemRow(
    item: ConversationPreview,
    onClick: () -> Unit
) {
    val timeFormatted = remember(item.lastMessageTime) {
        if (item.lastMessageTime == 0L) ""
        else {
            val now = System.currentTimeMillis()
            val diff = now - item.lastMessageTime
            if (diff < 86400000L) {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.lastMessageTime))
            } else {
                SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(item.lastMessageTime))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Round Avatar (Matching Screen 3)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (item.isGroup) Color(0xFF7C4DFF)
                    else try {
                        Color(android.graphics.Color.parseColor(item.avatarColorHex))
                    } catch (_: Exception) {
                        BbmTealPrimary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.isGroup) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = item.title.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF212121),
                    maxLines = 1
                )
                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = Color(0xFF78909C)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator (D / R badge or Lock for protected chat)
                if (item.title == "Patricia Padill" || item.lastMessage.contains("protected")) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Protected",
                        tint = BbmTealPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (item.isLastMessageOutgoing && item.lastMessageStatus != null) {
                    DeliveryTickBadge(status = item.lastMessageStatus)
                    Spacer(modifier = Modifier.width(5.dp))
                } else if (!item.isGroup && item.title == "Gideon Smith") {
                    // Delivery blue tick in preview matching Screen 3
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(StatusDeliveredBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }

                Text(
                    text = item.lastMessage,
                    fontSize = 13.sp,
                    color = Color(0xFF616161),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (item.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(BbmTealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.unreadCount}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic BBM Feeds Tab
 */
@Composable
private fun FeedsTabContent(
    onOpenChat: (String) -> Unit
) {
    val feeds = remember {
        listOf(
            Triple("Gideon Smith", "Changed status to: Available", "11:16 AM"),
            Triple("sales team", "Gideon Smith shared 'da-bbm-enterprise-sdk.pdf'", "11:15 AM"),
            Triple("Patricia Padill", "Security compliance validated • FIPS 140-2 certified", "11:14 AM"),
            Triple("Michael Accettura", "Updated status: great, thanks! :)", "Wed 2:03 PM"),
            Triple("PinPoint Enterprise", "DLP Policy synchronized. Communications end-to-end encrypted.", "Yesterday")
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(feeds) { (user, action, time) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (user == "Gideon Smith") onOpenChat("2B4F89A1")
                        else if (user == "sales team") onOpenChat("grp_sales_team")
                        else if (user == "Patricia Padill") onOpenChat("9C1288B4")
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BbmTealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = user,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = time,
                            fontSize = 11.sp,
                            color = Color(0xFF78909C)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = action,
                        fontSize = 12.sp,
                        color = Color(0xFF616161)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun ContactsTabContent(
    contacts: List<Contact>,
    invitations: List<ContactInvitation>,
    onOpenContact: (Contact) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onRejectInvite: (String) -> Unit,
    onAddNewContact: () -> Unit
) {
    val pendingInvites = invitations.filter { it.status == com.example.pinpoint.model.InvitationStatus.PENDING && it.isIncoming }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (pendingInvites.isNotEmpty()) {
            item {
                Text(
                    text = "Pending PIN Invitations (${pendingInvites.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BbmTealPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(pendingInvites) { inv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = inv.senderName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "PIN: ${inv.senderPin}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BbmTealPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = inv.inviteMessage,
                            fontSize = 13.sp,
                            color = Color(0xFF616161)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { onRejectInvite(inv.id) }) {
                                Text("Decline", color = Color(0xFF757575))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onAcceptInvite(inv.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary)
                            ) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contacts (${contacts.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                TextButton(onClick = onAddNewContact) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = BbmTealPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add by PIN", color = BbmTealPrimary)
                }
            }
        }

        items(contacts) { contact ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenContact(contact) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                                } catch (_: Exception) {
                                    BbmTealPrimary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.displayName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = contact.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF212121)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (contact.isOnline) StatusReadGreen else Color.Gray)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PIN: ${contact.pin}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = BbmTealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${contact.statusMessage}",
                                fontSize = 11.sp,
                                color = Color(0xFF757575),
                                maxLines = 1
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat",
                        tint = BbmTealPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupsTabContent(
    groups: List<GroupConversation>,
    onOpenGroup: (GroupConversation) -> Unit,
    onCreateGroup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Groups (${groups.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Button(
                    onClick = onCreateGroup,
                    colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary)
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Group")
                }
            }
        }

        items(groups) { group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGroup(group) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE0E0E0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF7C4DFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF212121)
                        )
                        Text(
                            text = "[${group.members.size}] members • Protected Group",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    userProfile: UserProfile,
    onUpdateName: (String) -> Unit,
    onUpdateStatus: (String) -> Unit,
    onSetupNewPin: () -> Unit,
    onOpenLegalCenter: () -> Unit,
    onOpenPicturePrivacy: () -> Unit
) {
    val context = LocalContext.current
    var editingName by remember { mutableStateOf(userProfile.displayName) }
    var editingStatus by remember { mutableStateOf(userProfile.statusMessage) }
    var isEditing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Picture & Audience Privacy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, BbmTealPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .border(3.dp, BbmTealPrimary, CircleShape)
                                .background(Color(0xFFE0F2FE))
                                .clickable { onOpenPicturePrivacy() }
                                .testTag("profile_avatar_tap"),
                            contentAlignment = Alignment.Center
                        ) {
                            val avatar = userProfile.avatarUri
                            if (avatar != null) {
                                if (avatar.startsWith("preset://ppm_logo") || avatar.startsWith("preset://")) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ppm_logo),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    AsyncImage(
                                        model = avatar,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                Text(
                                    text = userProfile.displayName.take(1).uppercase(),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BbmTealPrimary
                                )
                            }
                        }

                        // Camera / Edit Overlay Badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BbmTealPrimary)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { onOpenPicturePrivacy() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Edit Picture",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = userProfile.displayName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Audience Privacy Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when (userProfile.pictureVisibility) {
                            ProfilePictureVisibility.PUBLIC -> Color(0xFFE8F5E9)
                            ProfilePictureVisibility.CONTACTS_ONLY -> Color(0xFFE0F2FE)
                            ProfilePictureVisibility.PRIVATE_SELECTED -> Color(0xFFFFEBEE)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (userProfile.pictureVisibility) {
                                ProfilePictureVisibility.PUBLIC -> Color(0xFFA5D6A7)
                                ProfilePictureVisibility.CONTACTS_ONLY -> Color(0xFF90CAF9)
                                ProfilePictureVisibility.PRIVATE_SELECTED -> Color(0xFFFFCDD2)
                            }
                        ),
                        modifier = Modifier.clickable { onOpenPicturePrivacy() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (userProfile.pictureVisibility) {
                                    ProfilePictureVisibility.PUBLIC -> Icons.Default.Public
                                    ProfilePictureVisibility.CONTACTS_ONLY -> Icons.Default.People
                                    ProfilePictureVisibility.PRIVATE_SELECTED -> Icons.Default.Lock
                                },
                                contentDescription = null,
                                tint = when (userProfile.pictureVisibility) {
                                    ProfilePictureVisibility.PUBLIC -> Color(0xFF2E7D32)
                                    ProfilePictureVisibility.CONTACTS_ONLY -> BbmTealPrimary
                                    ProfilePictureVisibility.PRIVATE_SELECTED -> Color(0xFFC62828)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (userProfile.pictureVisibility) {
                                    ProfilePictureVisibility.PUBLIC -> "Photo Visibility: Public (Everyone)"
                                    ProfilePictureVisibility.CONTACTS_ONLY -> "Photo Visibility: My Contacts Only"
                                    ProfilePictureVisibility.PRIVATE_SELECTED -> "Photo Visibility: Private (${userProfile.allowedPersonPins.size} Persons, ${userProfile.allowedGroupIds.size} Groups)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (userProfile.pictureVisibility) {
                                    ProfilePictureVisibility.PUBLIC -> Color(0xFF2E7D32)
                                    ProfilePictureVisibility.CONTACTS_ONLY -> BbmTealPrimary
                                    ProfilePictureVisibility.PRIVATE_SELECTED -> Color(0xFFC62828)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenPicturePrivacy,
                        colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("button_change_photo_visibility")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (userProfile.avatarUri != null) "Change Photo & Audience Privacy" else "Add Picture & Set Privacy",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ppm_logo),
                        contentDescription = "PinPoint Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "My PinPoint Identity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QrCodeCanvas(
                        payload = "PIN:${userProfile.pin}",
                        sizeDp = 180.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier.border(1.dp, BbmTealPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PinPoint PIN", userProfile.pin))
                                    Toast.makeText(context, "PIN copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PIN: ${userProfile.pin}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = BbmTealPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy PIN",
                                tint = BbmTealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            onSetupNewPin()
                            Toast.makeText(context, "Verified unique 8-char PIN generated & assigned!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BbmTealPrimary),
                        modifier = Modifier.testTag("button_setup_unique_pin")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Unique PIN")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF212121)
                        )
                        IconButton(onClick = {
                            if (isEditing) {
                                onUpdateName(editingName)
                                onUpdateStatus(editingStatus)
                            }
                            isEditing = !isEditing
                        }) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save" else "Edit",
                                tint = BbmTealPrimary
                            )
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = editingName,
                            onValueChange = { editingName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editingStatus,
                            onValueChange = { editingStatus = it },
                            label = { Text("Status Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = userProfile.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userProfile.statusMessage,
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        // Required Legal Pages & Disclosures Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Legal Pages & Privacy Disclosures",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF212121)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Mandatory Google Play disclosures, privacy protections, data deletion portal, and terms.",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenLegalCenter,
                        colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("button_open_legal_center")
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Policy & Terms of Service")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenLegalCenter,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                        modifier = Modifier.fillMaxWidth().testTag("button_open_deletion_portal")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Account & Data Deletion Portal")
                    }
                }
            }
        }

        // Mandatory Powered by Workrr.in Hyperlink Footer
        item {
            val uriHandler = LocalUriHandler.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            uriHandler.openUri("https://workrr.in")
                        } catch (_: Exception) {
                            Toast.makeText(context, "Opening workrr.in", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 14.dp)
                    .testTag("profile_workrr_footer"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Powered by ",
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
                Text(
                    text = "Workrr.in",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BbmTealPrimary,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open workrr.in",
                    tint = BbmTealPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
