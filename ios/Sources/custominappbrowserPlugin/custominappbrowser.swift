import Foundation

@objc public class custominappbrowser: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
