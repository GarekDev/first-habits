package app.firstyou.habits.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.firstyou.habits.HabitsApp

@Composable
fun BackupScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text(
            text = "Copia de seguridad",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(12.dp))
        BackupControls()
    }
}

@Composable
fun BackupControls() {
    val context = LocalContext.current
    val app = context.applicationContext as HabitsApp
    val viewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.factory(app.container.repository),
    )
    val state by viewModel.state.collectAsState()
    val exportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportTo(context.contentResolver, uri)
    }
    val importFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.prepareImport(context.contentResolver, uri)
    }

    if (state.confirming) {
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Importar") },
            text = { Text("Esto sustituye los hábitos de este teléfono.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) { Text("Sustituir") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) { Text("Cancelar") }
            },
        )
    }

    Text(
        text = "El archivo guarda todos los hábitos, los días marcados, las cantidades, los colores y los recordatorios. Una carpeta cómoda es Documentos o Descargas.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.size(12.dp))
    Button(
        onClick = { exportFile.launch("first-habits-backup.json") },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Exportar")
    }
    Spacer(modifier = Modifier.size(12.dp))
    Button(
        onClick = { importFile.launch(arrayOf("application/json", "*/*")) },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Importar")
    }
    if (state.message != null) {
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = state.message!!,
            color = if (state.error) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
