package mil.nga.gars.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.math.RoundingMode;
import java.text.DecimalFormat;

import mil.nga.gars.GARS;
import mil.nga.gars.grid.GridType;
import mil.nga.gars.grid.style.Grid;
import mil.nga.gars.tile.GARSTileProvider;
import mil.nga.gars.tile.TileUtils;
import mil.nga.grid.features.Point;

/**
 * GARS Example Application
 *
 * @author osbornb
 */
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapClickListener {

    /**
     * Location permission request code
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Google map
     */
    private GoogleMap map;

    /**
     * GARS label
     */
    private TextView garsLabel;

    /**
     * WGS84 coordinate label
     */
    private TextView wgs84Label;

    /**
     * Zoom label
     */
    private TextView zoomLabel;

    /**
     * Search GARS result
     */
    private String searchGARSResult = null;

    /**
     * Coordinate label formatter
     */
    private final DecimalFormat coordinateFormatter = new DecimalFormat("0.0####");

    /**
     * Zoom level label formatter
     */
    private final DecimalFormat zoomFormatter = new DecimalFormat("0.0");

    /**
     * GARS tile provider
     */
    private GARSTileProvider tileProvider = null;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        garsLabel = findViewById(R.id.gars);
        wgs84Label = findViewById(R.id.wgs84);
        zoomLabel = findViewById(R.id.zoom);
        zoomFormatter.setRoundingMode(RoundingMode.DOWN);
        ImageButton searchButton = findViewById(R.id.search);
        searchButton.setOnClickListener(v -> onSearchClick());
        ImageButton mapTypeButton = findViewById(R.id.mapType);
        mapTypeButton.setOnClickListener(v -> onMapTypeClick());
        garsLabel.setOnLongClickListener(v -> {
            copyToClipboard(getString(R.string.gars_label), garsLabel.getText());
            return true;
        });
        wgs84Label.setOnLongClickListener(v -> {
            copyToClipboard(getString(R.string.wgs84_label), wgs84Label.getText());
            return true;
        });
        zoomLabel.setOnLongClickListener(v -> {
            copyToClipboard(getString(R.string.zoom_label), zoomLabel.getText());
            return true;
        });
        coordinateFormatter.setRoundingMode(RoundingMode.HALF_UP);

        tileProvider = GARSTileProvider.create(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
        map.setOnCameraIdleListener(this);
        map.setOnMapClickListener(this);
        enableMyLocation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCameraIdle() {
        CameraPosition cameraPosition = map.getCameraPosition();
        LatLng center = cameraPosition.target;
        float zoom = cameraPosition.zoom;
        String gars;
        if (searchGARSResult != null) {
            gars = searchGARSResult;
            searchGARSResult = null;
        } else {
            gars = tileProvider.getCoordinate(center, (int) zoom);
        }
        garsLabel.setText(gars);
        wgs84Label.setText(getString(R.string.wgs84_label_format,
                coordinateFormatter.format(center.longitude),
                coordinateFormatter.format(center.latitude)));
        zoomLabel.setText(zoomFormatter.format(zoom));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    /**
     * Handle map type click
     */
    private void onMapTypeClick() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.map_type_title);

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                new CharSequence[]{
                        getString(R.string.map_type_normal),
                        getString(R.string.map_type_satellite),
                        getString(R.string.map_type_terrain),
                        getString(R.string.map_type_hybrid)},
                map.getMapType() - 1,
                (dialog, item) -> {
                    map.setMapType(item + 1);
                    dialog.dismiss();
                }
        );

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

    }

    /**
     * Copy text to the clipboard
     *
     * @param label label
     * @param text  text
     */
    private void copyToClipboard(String label, CharSequence text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), getString(R.string.copied_message, label),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle search click
     */
    private void onSearchClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.search_title);
        final EditText input = new EditText(this);
        input.setSingleLine();
        builder.setView(input);

        builder.setPositiveButton(R.string.search, (dialog, which) -> search(input.getText().toString()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Search and move to the coordinate
     *
     * @param coordinate GARS or WGS84 coordinate
     */
    private void search(String coordinate) {
        searchGARSResult = null;
        Point point = null;
        Integer zoom = null;
        float currentZoom = map.getCameraPosition().zoom;
        try {
            coordinate = coordinate.trim();
            if (GARS.isGARS(coordinate)) {
                GARS gars = GARS.parse(coordinate);
                GridType gridType = GARS.precision(coordinate);
                point = gars.toPoint();
                searchGARSResult = coordinate.toUpperCase();
                zoom = garsCoordinateZoom(gridType, currentZoom);
            } else {
                String[] parts = coordinate.split("\\s*,\\s*");
                if (parts.length == 2) {
                    point = Point.point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                }
            }
        } catch (Exception e) {
            Log.e(MapsActivity.class.getSimpleName(),
                    "Unsupported coordinate: " + coordinate, e);
        }
        if (point != null) {
            LatLng latLng = TileUtils.toLatLng(point);
            if (searchGARSResult == null) {
                searchGARSResult = tileProvider.getCoordinate(latLng, (int) currentZoom);
            }
            CameraUpdate update;
            if (zoom != null) {
                update = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            } else {
                update = CameraUpdateFactory.newLatLng(latLng);
            }
            map.animateCamera(update);
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.search_error);
            alert.setMessage(coordinate);
            alert.setPositiveButton(R.string.ok, null);
            alert.setCancelable(true);
            alert.create().show();
        }
    }

    /**
     * Get the GARS coordinate zoom level
     *
     * @param gridType grid type precision
     * @param zoom     current zoom
     * @return zoom level or null
     */
    private Integer garsCoordinateZoom(GridType gridType, float zoom) {
        Integer garsZoom = null;
        Grid grid = tileProvider.getGrid(gridType);
        int minZoom = grid.getLinesMinZoom();
        if (zoom < minZoom) {
            garsZoom = minZoom;
        } else {
            Integer maxZoom = grid.getLinesMaxZoom();
            if (maxZoom != null && zoom >= maxZoom + 1) {
                garsZoom = maxZoom;
            }
        }
        return garsZoom;
    }

    /**
     * Enables the My Location layer if fine or coarse location permission has been granted.
     */
    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        } else {
            // Location permission has not been granted yet, request it.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) || isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            enableMyLocation();
        }
    }

    /**
     * Is permission granted
     *
     * @param permissions  permissions
     * @param grantResults grant results
     * @param permission   permission
     * @return true if granted
     */
    private static boolean isPermissionGranted(String[] permissions, int[] grantResults,
                                               String permission) {
        for (int i = 0; i < permissions.length; i++) {
            if (permission.equals(permissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

}
