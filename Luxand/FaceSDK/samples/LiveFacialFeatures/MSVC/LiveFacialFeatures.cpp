// FaceTracking.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "LuxandFaceSDK.h"

int _tmain(int argc, _TCHAR* argv[])
{
    if (FSDKE_OK != FSDK_ActivateLibrary("aCGamccfB6Uj3vlS7eDEryPnDrTbrZQb77ZHouPl3J8Q7o+BG4PcGevchFjppkWrVa038OU6Fghhy/BJfJV1n82InviCSijl8Vbxb11fs+VrcbSEfpESqjKSJQK8OLCqU0qYDy1oRHLRAg/3CHKCBzP/6IHuamy9Y/aY/xd1E7A=")) {
		MessageBox(0, L"Please run the License Key Wizard (Start - Luxand - FaceSDK - License Key Wizard)\n", L"Error activating FaceSDK", MB_ICONERROR | MB_OK);
        exit(-1);
    }
	FSDK_Initialize("");
	FSDK_InitializeCapturing();

    int CameraCount;
    wchar_t ** CameraList;
    if (0 == FSDK_GetCameraList(&CameraList, &CameraCount))
		for (int i = 0; i < CameraCount; i++) 
			wprintf(L"camera: %s\n", CameraList[i]);

	if (0 == CameraCount) {
		MessageBox(0, L"Please attach a camera", L"Error", MB_ICONERROR | MB_OK);
        return -1;
    }

	int CameraIdx = 0; // choose the first camera
	int VideoFormatCount = 0;
	FSDK_VideoFormatInfo * VideoFormatList = 0;
	FSDK_GetVideoFormatList(CameraList[CameraIdx], &VideoFormatList, &VideoFormatCount);
	for (int i = 0; i < VideoFormatCount ; i++) 
		printf("format %d: %dx%d, %d bpp\n", i, VideoFormatList[i].Width, VideoFormatList[i].Height, VideoFormatList[i].BPP);

	int VideoFormat = 0; // choose a video format
	int width = VideoFormatList[VideoFormat].Width;
	int height = VideoFormatList[VideoFormat].Height;
	int bitsPerPixel = VideoFormatList[VideoFormat].BPP;
	FSDK_SetVideoFormat(CameraList[CameraIdx], VideoFormatList[VideoFormat]);

	printf("Trying to open the first camera...\n");
	int cameraHandle = 0;
	if (FSDKE_OK != FSDK_OpenVideoCamera(CameraList[CameraIdx], &cameraHandle))		{ 
		MessageBox(0, L"Error opening the first camera", L"Error", MB_ICONERROR | MB_OK);
        return -2;
	} 

	// creating a Tracker
	HTracker tracker = 0;
	FSDK_CreateTracker(&tracker);

	int err = 0; // set realtime face detection parameters
	FSDK_SetTrackerMultipleParameters(tracker, "RecognizeFaces=false; DetectFacialFeatures=true; HandleArbitraryRotations=false; DetermineFaceRotationAngle=false; InternalResizeWidth=100; FaceDetectionThreshold=5;", &err);

	// creating a window
	HWND hwnd = CreateWindowEx(WS_EX_TOOLWINDOW, L"LISTBOX", L"LiveFacialFeatures", 0, 0, 0, 0, 0, 0, 0, 0, 0); 
	HDC dc = GetDC(hwnd);
	SetWindowPos(hwnd, 0, 0, 0, 6+width, 6+32+(height), SWP_NOZORDER|SWP_NOMOVE);
	ShowWindow(hwnd, SW_SHOW);

	HPEN FaceRectanglePen = CreatePen(PS_SOLID, 1, RGB(0, 255, 0));
	HBRUSH FaceRectangleBrush = (HBRUSH)GetStockObject(NULL_BRUSH);

	HPEN FeatureCirclesPen = CreatePen(PS_SOLID, 1, RGB(0, 0, 255));
	LOGBRUSH brush;
	brush.lbColor = RGB(0, 0, 255);
	brush.lbStyle = BS_SOLID;
	HBRUSH FeatureCirclesBrush = CreateBrushIndirect(&brush);

	SendMessage(hwnd, LB_ADDSTRING, 0, (LPARAM)(L"Press Esc to exit ..."));
						
	MSG msg = {0};
	while(msg.message != WM_QUIT) {
		HImage imageHandle;
		if (FSDK_GrabFrame(cameraHandle, &imageHandle) == FSDKE_OK) { // grab the current frame from the camera
			long long IDs[256];
			long long faceCount = 0;
			FSDK_FeedFrame(tracker, 0, imageHandle, &faceCount, IDs, sizeof(IDs));

			HBITMAP hbitmapHandle; // to store the HBITMAP handle
			FSDK_SaveImageToHBitmap(imageHandle, &hbitmapHandle);

			DrawState(dc, NULL, NULL, (LPARAM)hbitmapHandle, NULL, 0, 16, width, height, DST_BITMAP | DSS_NORMAL);		
					
			for (int i = 0; i < faceCount; i++) {
				TFacePosition facePosition;
				FSDK_Features facialFeatures;

				FSDK_GetTrackerFacePosition(tracker, 0, IDs[i], &facePosition);
				FSDK_GetTrackerFacialFeatures(tracker, 0, IDs[i], &facialFeatures);

				int x1 = facePosition.xc - (int)(facePosition.w*0.6);
				int y1 = facePosition.yc - (int)(facePosition.w*0.5);
				int x2 = facePosition.xc + (int)(facePosition.w*0.6);
				int y2 = facePosition.yc + (int)(facePosition.w*0.7);

				SelectObject(dc, FaceRectanglePen);
				SelectObject(dc, FaceRectangleBrush);
				Rectangle(dc, x1, 16 + y1, x2, 16 + y2);

				SelectObject(dc, FeatureCirclesPen);
				SelectObject(dc, FeatureCirclesBrush);
				for (int i = 0; i < FSDK_FACIAL_FEATURE_COUNT; i++)
					Ellipse(dc, facialFeatures[i].x - 2, 16 + facialFeatures[i].y-2, facialFeatures[i].x + 2, 16 + facialFeatures[i].y + 2);
			}
			
			DeleteObject(hbitmapHandle); // delete the HBITMAP object
			FSDK_FreeImage(imageHandle);// delete the FSDK image handle
		};

		if (PeekMessage(&msg, 0, 0, 0, PM_REMOVE))
		{
			TranslateMessage(&msg);   
			DispatchMessage(&msg); 
			if (msg.message == WM_KEYDOWN && msg.wParam == VK_ESCAPE) 
				break;
		}
	}

	ReleaseDC(hwnd, dc);
	FSDK_FreeTracker(tracker);

	if (FSDKE_OK != FSDK_CloseVideoCamera(cameraHandle)) {
		MessageBox(0, L"Error closing camera", L"Error", MB_ICONERROR | MB_OK);
        return -5;
	}
	FSDK_FreeVideoFormatList(VideoFormatList);
	FSDK_FreeCameraList(CameraList, CameraCount);

	FSDK_FinalizeCapturing();
	FSDK_Finalize();
	return 0;
}

