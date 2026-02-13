package com.neomud.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.neomud.client.ui.navigation.NeoMudNavGraph
import com.neomud.client.ui.theme.NeoMudTheme
import com.neomud.client.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeoMudTheme {
                NeoMudNavGraph(authViewModel = authViewModel)
            }
        }
    }
}
