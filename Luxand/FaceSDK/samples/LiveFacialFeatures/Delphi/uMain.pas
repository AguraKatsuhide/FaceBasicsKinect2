unit uMain;

interface

uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, ExtCtrls, StdCtrls, LuxandFaceSDK;

type
  TForm1 = class(TForm)
    Button1: TButton;
    Image1: TImage;
    procedure Button1Click(Sender: TObject);
    procedure OnClose(Sender: TObject; var Action: TCloseAction);
    procedure OnCreate(Sender: TObject);
  private
    { Private declarations }
  public
    { Public declarations }
  end;

var
  Form1: TForm1;
  NeedInterrupt: boolean;
  cameraHandle: integer;

implementation

{$R *.dfm}
{$O-}

procedure TForm1.OnCreate(Sender: TObject);
var
  CameraList: PFSDK_CameraList;
  CameraCount: integer;
  VideoFormatList: PFSDK_VideoFormatInfoArray;
  VideoFormatCount: integer;
begin
  if FSDK_ActivateLibrary(PAnsiChar(AnsiString('aCGamccfB6Uj3vlS7eDEryPnDrTbrZQb77ZHouPl3J8Q7o+BG4PcGevchFjppkWrVa038OU6Fghhy/BJfJV1n82InviCSijl8Vbxb11fs+VrcbSEfpESqjKSJQK8OLCqU0qYDy1oRHLRAg/3CHKCBzP/6IHuamy9Y/aY/xd1E7A=')))<>FSDKE_OK then
  begin
    Application.MessageBox('Please run the License Key Wizard (Start - Luxand - FaceSDK - License Key Wizard)','Error activating FaceSDK');
    exit;
  end;

  FSDK_Initialize('');
  FSDK_InitializeCapturing();

  CameraList := nil;
  FSDK_GetCameraList(@CameraList, @CameraCount);
    VideoFormatList := nil;
    VideoFormatCount := 0;
    FSDK_GetVideoFormatList(CameraList[0], @VideoFormatList, @VideoFormatCount);

  image1.Height := VideoFormatList[0].Height;
  image1.Width := VideoFormatList[0].Width;
  image1.Canvas.Pen.Width := 1;

  self.Width := image1.Width+30;
  self.Height := image1.Height+90;

  button1.Left := (self.Width div 2)-40;
  button1.Width := 80;
  button1.Top := self.Height-75;
  button1.Height := 25;

  FSDK_SetVideoFormat(CameraList[0], VideoFormatList[0]);

  if (FSDK_OpenVideoCamera(CameraList[0], @cameraHandle) < 0) then
  begin
    Application.MessageBox('Error opening camera','Error');
    FSDK_Finalize;
    Application.Terminate;
  end;
end;

procedure TForm1.Button1Click(Sender: TObject);
var
  bmp1:TBitMap;
  hbitmapHandl:HBitMap; // to store the HBITMAP handle
  imageHandle: HImage;
  faceCoords: TFacePosition;
  facialFeatures: FSDK_Features;
  left, top, right, bottom: integer;
  i, j: integer;
  tracker: integer;
  err: integer;
  IDs: array[0..255] of int64;
  faceCount: integer;

begin
  button1.Enabled := false;
  NeedInterrupt := false;

  tracker := 0;
  FSDK_CreateTracker(@tracker);

  err := 0; // set realtime face detection parameters
  FSDK_SetTrackerMultipleParameters(tracker, 'RecognizeFaces=false; DetectFacialFeatures=true; HandleArbitraryRotations=false; DetermineFaceRotationAngle=false; InternalResizeWidth=100; FaceDetectionThreshold=5;', @err);

  while not NeedInterrupt do
  begin
    if FSDKE_OK <> FSDK_GrabFrame(cameraHandle, @imageHandle) then // grab the current frame from the camera
    begin
      application.ProcessMessages;
      continue;
    end;

		faceCount := 0;
    FSDK_FeedFrame(tracker, 0, imageHandle, @faceCount, @IDs, sizeof(IDs)); // maximum 256 faces detected

    FSDK_SaveImageToHbitmap(imageHandle, @hbitmapHandl);

    bmp1 := TBitMap.Create;
    bmp1.Handle := hbitmapHandl;

    // display current frame
    image1.Canvas.Draw(0, 0, bmp1);

    for i:= 0 to faceCount-1 do
    begin
      FSDK_GetTrackerFacePosition(tracker, 0, IDs[i], @faceCoords);
      FSDK_GetTrackerFacialFeatures(tracker, 0, IDs[i], @facialFeatures);

      left := faceCoords.xc - round(faceCoords.w*0.6);
      top := faceCoords.yc - round(faceCoords.w*0.5);
      right := faceCoords.xc + round(faceCoords.w*0.6);
      bottom := faceCoords.yc + round(faceCoords.w*0.7);

      image1.Canvas.Brush.Style := bsClear;
      image1.Canvas.Pen.Color := clLime;
      image1.Canvas.Rectangle(left, top, right, bottom);
      image1.Canvas.Brush.Style := bsSolid;
      image1.Canvas.Brush.Color := clBlue;
      image1.Canvas.Pen.Color := clBlue;
      for j := 0 to FSDK_FACIAL_FEATURE_COUNT - 1 do
        image1.Canvas.Ellipse(facialFeatures[j].x - 2, facialFeatures[j].y - 2, facialFeatures[j].x + 2, facialFeatures[j].y + 2);
    end;

    // make UI controls accessible
    application.processmessages;
    sleep(10);

    bmp1.Free; // delete the TBitMap object
    FSDK_FreeImage(imageHandle); // delete the FSDK image handle
  end;

  FSDK_CloseVideoCamera(cameraHandle);
  FSDK_FreeTracker(tracker);
  FSDK_Finalize;
end;

procedure TForm1.OnClose(Sender: TObject; var Action: TCloseAction);
begin
  NeedInterrupt := true;
end;

end.
