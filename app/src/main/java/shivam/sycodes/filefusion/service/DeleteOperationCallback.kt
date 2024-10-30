package shivam.sycodes.filefusion.service

interface DeleteOperationCallback {
    fun onSuccess(deletedFiles: List<String>)
    fun onFailure(errorMessage: String)
}