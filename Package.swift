// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Custominappbrowser",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "Custominappbrowser",
            targets: ["custominappbrowserPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "custominappbrowserPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/custominappbrowserPlugin"),
        .testTarget(
            name: "custominappbrowserPluginTests",
            dependencies: ["custominappbrowserPlugin"],
            path: "ios/Tests/custominappbrowserPluginTests")
    ]
)