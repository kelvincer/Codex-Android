package com.example.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.ui.theme.InventoryTheme
import java.text.NumberFormat
import java.util.Locale

private const val IGV_RATE = 0.18

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InventoryTheme(darkTheme = false) {
                InventoryApp()
            }
        }
    }
}

data class Product(
    val id: Int,
    val name: String,
    val quantity: Int,
    val pricePerUnit: Double
)

sealed class InventoryScreen(val route: String, val label: String) {
    data object Sell : InventoryScreen("sell", "Vender")
    data object Add : InventoryScreen("add", "Ingresar")
}

@Composable
fun InventoryApp() {
    val navController = rememberNavController()
    val items = remember {
        mutableStateListOf(
            Product(1, "Café premium", 18, 12.5),
            Product(2, "Galletas integrales", 30, 4.2),
            Product(3, "Aceite de oliva", 12, 28.9)
        )
    }

    val screens = listOf(InventoryScreen.Sell, InventoryScreen.Add)
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inventario del negocio",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (screen is InventoryScreen.Sell) {
                                    Icons.Default.PointOfSale
                                } else {
                                    Icons.Default.Inventory
                                },
                                contentDescription = null
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = InventoryScreen.Sell.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(InventoryScreen.Sell.route) {
                SellScreen(items = items)
            }
            composable(InventoryScreen.Add.route) {
                AddProductScreen(items = items)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(items: MutableList<Product>) {
    val selectedIndex = rememberSaveable { mutableStateOf(0) }
    val quantityToSell = rememberSaveable { mutableStateOf(1) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    if (items.isEmpty()) {
        EmptyInventoryState()
        return
    }

    val selectedProduct = items[selectedIndex.value]
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "PE")) }
    val priceWithIgv = selectedProduct.pricePerUnit * (1 + IGV_RATE)

    LaunchedEffect(selectedProduct.id) {
        quantityToSell.value = 1
        errorMessage.value = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Venta",
            style = MaterialTheme.typography.headlineSmall
        )

        ProductDropdown(
            items = items,
            selectedIndex = selectedIndex
        )

        OutlinedTextField(
            value = buildString {
                appendLine("Nombre: ${selectedProduct.name}")
                appendLine("Cantidad: ${selectedProduct.quantity}")
                append("Precio con IGV: ${currencyFormat.format(priceWithIgv)}")
            },
            onValueChange = {},
            readOnly = true,
            label = { Text("Detalle del producto") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        QuantityField(
            label = "Cantidad a vender",
            quantityState = quantityToSell
        )

        Button(
            onClick = {
                val requested = quantityToSell.value
                if (requested <= 0) {
                    errorMessage.value = "La cantidad debe ser mayor a cero."
                    return@Button
                }
                if (requested > selectedProduct.quantity) {
                    errorMessage.value = "No hay suficiente stock para esta venta."
                    return@Button
                }

                items[selectedIndex.value] = selectedProduct.copy(
                    quantity = selectedProduct.quantity - requested
                )
                errorMessage.value = "Venta registrada y stock actualizado."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar venta")
        }

        errorMessage.value?.let {
            Text(
                text = it,
                color = if (it.contains("actualizado")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        InventoryList(items = items)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDropdown(
    items: List<Product>,
    selectedIndex: MutableState<Int>
) {
    val expanded = remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = items[selectedIndex.value].name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Producto") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Text("▾") }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, product ->
                DropdownMenuItem(
                    text = { Text(product.name) },
                    onClick = {
                        selectedIndex.value = index
                        expanded.value = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { expanded.value = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar producto")
        }
    }
}

@Composable
fun InventoryList(items: List<Product>) {
    Column {
        Text(
            text = "Inventario actualizado",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { product ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                            Text("Stock: ${product.quantity}")
                        }
                        Text("S/ ${"%.2f".format(product.pricePerUnit)}")
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductScreen(items: MutableList<Product>) {
    val name = rememberSaveable { mutableStateOf("") }
    val quantity = rememberSaveable { mutableStateOf(0) }
    val price = rememberSaveable { mutableStateOf(0.0) }
    val message = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ingresar productos",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Nombre del producto") },
            modifier = Modifier.fillMaxWidth()
        )

        QuantityField(
            label = "Cantidad de artículos",
            quantityState = quantity
        )

        PriceField(
            label = "Precio por artículo",
            priceState = price
        )

        Button(
            onClick = {
                if (name.value.isBlank()) {
                    message.value = "Ingresa un nombre válido."
                    return@Button
                }
                if (quantity.value <= 0) {
                    message.value = "La cantidad debe ser mayor a cero."
                    return@Button
                }
                if (price.value <= 0) {
                    message.value = "El precio debe ser mayor a cero."
                    return@Button
                }

                val nextId = (items.maxOfOrNull { it.id } ?: 0) + 1
                items.add(
                    Product(
                        id = nextId,
                        name = name.value.trim(),
                        quantity = quantity.value,
                        pricePerUnit = price.value
                    )
                )
                name.value = ""
                quantity.value = 0
                price.value = 0.0
                message.value = "Producto agregado al inventario."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar producto")
        }

        message.value?.let {
            Text(
                text = it,
                color = if (it.contains("agregado")) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        InventoryList(items = items)
    }
}

@Composable
fun QuantityField(
    label: String,
    quantityState: MutableState<Int>
) {
    OutlinedTextField(
        value = if (quantityState.value == 0) "" else quantityState.value.toString(),
        onValueChange = { newValue ->
            quantityState.value = newValue.toIntOrNull() ?: 0
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PriceField(
    label: String,
    priceState: MutableState<Double>
) {
    OutlinedTextField(
        value = if (priceState.value == 0.0) "" else priceState.value.toString(),
        onValueChange = { newValue ->
            priceState.value = newValue.replace(',', '.').toDoubleOrNull() ?: 0.0
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun EmptyInventoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No hay productos cargados.")
    }
}
