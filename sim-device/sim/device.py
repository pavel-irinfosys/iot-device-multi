
class Device:

    def __init__(self, imei: str, upload_interval_ms: int):

        self.__imei:str = imei
        self.__interval_ms:int = upload_interval_ms
        self.__last_upload_time = 0

    def is_ready_for_upload(self, current_time: int):

        if current_time - self.__last_upload_time >= self.__interval_ms:
            self.__last_upload_time = current_time
            return True
        return False


    @property
    def imei(self) -> str:
        return self.__imei

