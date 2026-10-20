import os

# Target directory path
target_dir = "composeApp/src/commonMain/composeResources/drawable"

# List of missing icons based on the app screenshots
icons = [
    "ancestors_photograph", "shoe_rack", "heavy_storage_storeroom", 
    "light_storage", "pet_house_cage", "swimming_pool", 
    "broom_mop", "sewing_machine", "ironing_board", "bookshelf", 
    "television", "computer_laptop", "heater_fireplace", 
    "air_conditioner", "wall_clock", "family_photograph", 
    "borewell_tubewell", "first_aid_kit", "indoor_plants", 
    "study_desk", "meditation_mat", "business_cash_counter", 
    "investment_share_files", "microwave_oven", "inverter_generator", 
    "master_bedroom", "childrens_bedroom", "guest_bedroom", 
    "living_drawing_room", "dining_room", "main_entrance", 
    "staircase", "balcony_terrace", "bathroom_bathing_only", 
    "washing_machine"
]

# Boilerplate Android Vector Drawable XML (Placeholder Triangle)
vector_xml = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24.0"
    android:viewportHeight="24.0">
    <path
        android:fillColor="#8D7A6F"
        android:pathData="M12,2 L2,22 h20 L12,2 z" />
</vector>"""

def generate_drawables():
    # Ensure the directory exists
    os.makedirs(target_dir, exist_ok=True)
    
    # Generate the XML files
    for icon in icons:
        file_path = os.path.join(target_dir, f"{icon}.xml")
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(vector_xml)
        print(f"Generated: {file_path}")

if __name__ == "__main__":
    generate_drawables()
    print("All placeholder drawables generated successfully.")
