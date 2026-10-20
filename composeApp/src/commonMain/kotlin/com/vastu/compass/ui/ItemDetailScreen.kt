private fun getDetailVectorIcon(iconKey: String): ImageVector {
    val key = iconKey.lowercase()
    return when {
        // Water & Plumbing
        key.contains("water") || key.contains("borewell") || key.contains("tubewell") -> Icons.Default.WaterDrop
        key.contains("filter") || key.contains("ro") || key.contains("drink") -> Icons.Default.LocalDrink
        key.contains("wc") || key.contains("toilet") || key.contains("bath") -> Icons.Default.Wc
        
        // Rooms & Doors
        key.contains("bed") || key.contains("guest") -> Icons.Default.Bed
        key.contains("gate") || key.contains("entrance") || key.contains("door") -> Icons.Default.MeetingRoom
        key.contains("stair") -> Icons.Default.Menu
        key.contains("balcony") || key.contains("terrace") -> Icons.Default.Home
        
        // Electronics & Appliances
        key.contains("tv") || key.contains("television") -> Icons.Default.Tv
        key.contains("laptop") || key.contains("computer") -> Icons.Default.Laptop
        key.contains("oven") || key.contains("microwave") || key.contains("fridge") -> Icons.Default.Kitchen
        key.contains("invertor") || key.contains("generator") || key.contains("power") -> Icons.Default.Power
        key.contains("heat") || key.contains("fire") -> Icons.Default.Whatshot
        key.contains("ac") || key.contains("conditioner") -> Icons.Default.AcUnit
        
        // Furniture & Objects
        key.contains("desk") || key.contains("study") || key.contains("bookshelf") || key.contains("book") -> Icons.Default.MenuBook
        key.contains("clock") || key.contains("time") -> Icons.Default.Schedule
        key.contains("shoe") -> Icons.Default.Layers
        key.contains("sew") || key.contains("machine") -> Icons.Default.ContentCut
        key.contains("iron") -> Icons.Default.Checkroom
        key.contains("broom") || key.contains("mop") || key.contains("clean") -> Icons.Default.CleaningServices
        key.contains("mirror") || key.contains("face") -> Icons.Default.Face
        
        // Photos & Items
        key.contains("ancestor") -> Icons.Default.Portrait
        key.contains("family") || key.contains("photo") -> Icons.Default.Image
        key.contains("medic") || key.contains("first aid") || key.contains("health") -> Icons.Default.MedicalServices
        key.contains("invest") || key.contains("share") || key.contains("file") -> Icons.Default.TrendingUp
        
        // Storage
        key.contains("heavy") || key.contains("storage") || key.contains("store") -> Icons.Default.Storage
        key.contains("light") || key.contains("inbox") -> Icons.Default.AllInbox
        
        // Outdoors & Misc
        key.contains("plant") || key.contains("indoor") || key.contains("park") -> Icons.Default.Park
        key.contains("pet") || key.contains("cage") -> Icons.Default.Pets
        key.contains("pool") || key.contains("swim") -> Icons.Default.Pool
        key.contains("car") || key.contains("parking") -> Icons.Default.DirectionsCar
        
        // Spiritual & Living
        key.contains("mandir") || key.contains("temple") || key.contains("pooja") -> Icons.Default.Brightness7
        key.contains("delete") || key.contains("dustbin") || key.contains("trash") -> Icons.Default.Delete
        key.contains("lock") || key.contains("safe") -> Icons.Default.Lock
        key.contains("kitchen") || key.contains("stove") || key.contains("counter") -> Icons.Default.Countertops
        key.contains("sofa") || key.contains("living") || key.contains("lounge") -> Icons.Default.Weekend
        
        // Generic Categories
        key.contains("money") || key.contains("finance") -> Icons.Default.Payments
        key.contains("love") || key.contains("marriage") -> Icons.Default.Favorite
        key.contains("career") || key.contains("job") || key.contains("work") -> Icons.Default.Work
        key.contains("child") || key.contains("baby") -> Icons.Default.ChildCare
        
        else -> Icons.Default.Category
    }
}
